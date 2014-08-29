package com.etsy.conjecture.scalding

import org.apache.commons.math3.linear._

import com.etsy.scalding._
import com.twitter.algebird.Operators._
import com.twitter.scalding._

import cascading.flow.FlowDef
import cascading.pipe.Pipe
import cascading.pipe.joiner.InnerJoin
import cascading.tuple.Fields

object NNMF extends Serializable {

  import com.twitter.scalding.Dsl._

  // based on http://research.microsoft.com/pubs/119077/dnmf.pdf

  /**
   * input:
   * A: a sparse matrix in the form ('row, 'col, 'val), with tuples of type (R, C, Double).
   * k: the dimension of the factorization.
   */
  def initGaussian(A : Pipe, k : Int, reducers : Int = 500) : (Pipe, Pipe) = {
    val H0 = A.groupBy('row){_.size('count).reducers(reducers)}
      .map(() -> 'vec){_ : Unit => MatrixUtils.createRealVector((0 until k).map{i => math.random}.toArray)}
      .map(() -> 'bias){_ : Unit => math.random}
      .project('row, 'vec, 'bias)
    val W0 = A.groupBy('col){_.size('count).reducers(reducers)}
      .map(() -> 'vec){_ : Unit => MatrixUtils.createRealVector((0 until k).map{i => math.random}.toArray)}
      .map(() -> 'bias){_ : Unit => math.random}
      .project('col, 'vec, 'bias)
    (H0, W0)
  }

  /**
   * These functions embed bias terms for both factors into the original factorization.
   */
  def createWVector(v : RealVector, b : Double) : RealVector = {
    v.append(MatrixUtils.createRealVector(Array(1.0, b)))
  }

  def createHVector(v : RealVector, b : Double) : RealVector = {
    v.append(MatrixUtils.createRealVector(Array(b, 1.0)))
  }

  def explodeWVector(u : RealVector) : (RealVector, Double) = {
    val d = u.getDimension
    (u.getSubVector(0, d - 2), u.getEntry(d - 1))
  }

  def explodeHVector(u : RealVector) : (RealVector, Double) = {
    val d = u.getDimension
    (u.getSubVector(0, d - 2), u.getEntry(d - 2))
  }

  /*
   * input:
   * A: a sparse matrix in the form ('row, 'col, 'val), with tuples of type (R, C, Double).
   * H: a dense matrix of ('row, 'vec, 'bias)
   * W: a dense matrix of ('col, 'vec, 'bias)
   * With W,H from initGaussian or a previous iteration.
   */
  def updateGaussian(A : Pipe, H : Pipe, W : Pipe, reducers : Int = 500) : (Pipe, Pipe) = {

    // Note that row and column vectors are both represented as a RealVector which doesnt have an orientation.
    // Therefore whether it is a row or column will have to be inferred from context.

    // -- First update H.
    // W'W
    val WW = W.mapTo(('vec, 'bias) -> 'WW){v : (RealVector, Double) =>
        val u = createWVector(v._1, v._2)
        u.outerProduct(u)
      }
      .groupAll{_.reduce[RealMatrix]('WW){(a, b) => a.add(b)}}

    // W'WH
    val WWH = H.crossWithTiny(WW)
      .map(('WW, 'vec, 'bias) -> 'vec_wwh){x : (RealMatrix, RealVector, Double) => x._1.operate(createHVector(x._2, x._3))}
      .project('row, 'vec_wwh)

    // W'A
    val WA = W.joinWithLarger('col -> 'col, A, new InnerJoin(), reducers)
      .map(('val, 'vec, 'bias) -> 'vec){x : (Double, RealVector, Double) => createWVector(x._2, x._3).mapMultiply(x._1)}
      .groupBy('row){_.reduce[RealVector]('vec -> 'vec_wa){(a, b) => a.add(b)}.reducers(reducers).forceToReducers}

    // Pointwise multiplier to old H
    val HM = WA.joinWithSmaller('row -> 'row, WWH, new InnerJoin(), reducers)
      .map(('vec_wa, 'vec_wwh) -> 'vec_mult){x : (RealVector, RealVector) => x._1.ebeDivide(x._2)}
      .map('vec_mult -> 'vec_mult){x : RealVector => MatrixUtils.createRealVector(x.toArray.map{i => if(i.isInfinite || i.isNaN) 1.0 else i})}
      .project('row, 'vec_mult)

    // new H.
    val H_ = H.joinWithSmaller('row -> 'row, HM, new InnerJoin(), reducers)
      .map(('vec, 'bias, 'vec_mult) -> ('vec, 'bias)){x : (RealVector, Double, RealVector) => explodeHVector(createHVector(x._1, x._2).ebeMultiply(x._3))}
      .project('row, 'vec, 'bias)

    // -- Then update W.
    // HH'
    val HH = H_.mapTo(('vec, 'bias) -> 'HH){v : (RealVector, Double) =>
        val u = createHVector(v._1, v._2)
        u.outerProduct(u)
      }
      .groupAll{_.reduce[RealMatrix]('HH){(a, b) => a.add(b)}}

    // WHH'
    val WHH = W.crossWithTiny(HH)
      .map(('HH, 'vec, 'bias) -> 'vec_whh){x : (RealMatrix, RealVector, Double) => x._1.operate(createWVector(x._2, x._3))}
      .project('col, 'vec_whh)

    // AH'
    val AH = H_.joinWithLarger('row -> 'row, A, new InnerJoin(), reducers)
      .map(('val, 'vec, 'bias) -> 'vec){x : (Double, RealVector, Double) => createHVector(x._2, x._3).mapMultiply(x._1)}
      .groupBy('col){_.reduce[RealVector]('vec -> 'vec_ah){(a, b) => a.add(b)}.reducers(reducers).forceToReducers}

    // Pointwise multiplier to old W
    val WM = AH.joinWithSmaller('col -> 'col, WHH, new InnerJoin(), reducers)
      .map(('vec_ah, 'vec_whh) -> 'vec_mult){x : (RealVector, RealVector) => x._1.ebeDivide(x._2)}
      .map('vec_mult -> 'vec_mult){x : RealVector => MatrixUtils.createRealVector(x.toArray.map{i => if(i.isInfinite || i.isNaN) 1.0 else i})}
      .project('col, 'vec_mult)

    // new W.
    val W_ = W.joinWithSmaller('col -> 'col, WM, new InnerJoin(), reducers)
      .map(('vec, 'bias, 'vec_mult) -> ('vec, 'bias)){x : (RealVector, Double, RealVector) => explodeWVector(createWVector(x._1, x._2).ebeMultiply(x._3))}
      .project('col, 'vec, 'bias)

    (H_, W_)
  }

  /**
   * Possibly faster vec add that doesnt create a new object.
   */
  def addTo(acc : RealVector, v : RealVector) : RealVector = {
    acc.combineToSelf(1.0, 1.0, v)
    acc
  }

  /**
   * Possibly faster matrix  add that doesnt create a new object.
   */
  def addTo(acc : RealMatrix, m : RealMatrix) : RealMatrix = {
    var r = 0
    while(r < acc.getRowDimension) {
      var c = 0
      while(c < acc.getColumnDimension) {
        acc.addToEntry(r, c, m.getEntry(r, c))
        c += 1
      }
      r += 1
    }
    acc
  }

  /*
   * -- weighted version.
   * Optimizes weighted l2 loss, where zeros have weight 1, non zeros have weight 1+alpha.
   * input:
   * A: a sparse matrix in the form ('row, 'col, 'val), with tuples of type (R, C, Double).
   * H: a dense matrix of ('row, 'vec, 'bias)
   * W: a dense matrix of ('col, 'vec, 'bias)
   * alpha: alpha from loss function.
   * With W,H from initGaussian or a previous iteration.
   */
  def updateGaussianWeighted(A : Pipe, H : Pipe, W : Pipe, alpha : Double, reducers : Int = 500) : (Pipe, Pipe) = {

    // -- First update H.
    // W'W
    val WW = W.mapTo(('vec, 'bias) -> 'WW){v : (RealVector, Double) =>
        val u = createWVector(v._1, v._2)
        u.outerProduct(u)
      }
      .groupAll{_.reduce[RealMatrix]('WW){(a, b) => a.add(b)}}

    // W'WH
    val WWH = H.crossWithTiny(WW)
      .map(('vec, 'bias) -> 'vec){x : (RealVector, Double) => createHVector(x._1, x._2)}
      .map(('WW, 'vec) -> 'vec_wwh){x : (RealMatrix, RealVector) => x._1.operate(x._2)}
      .project('row, 'vec_wwh, 'vec)

    // W'A
    val WA = W.joinWithLarger('col -> 'col, A, new InnerJoin(), reducers)
      .map(('val, 'vec, 'bias) -> ('vec_wa, 'denom_vec)){x : (Double, RealVector, Double) =>
        val v = createWVector(x._2, x._3)
        (v.mapMultiply(x._1), v)
      }
      .groupBy('row){
        _.reduce[RealVector]('vec_wa){(a, b) => addTo(a, b)}
        .toList[RealVector]('denom_vec -> 'denom_vec_list)
        .reducers(reducers)
        //.forceToReducers
      }
      .project('row, 'vec_wa, 'denom_vec_list)

    // Pointwise multiplier to old H
    val HM = WA.joinWithSmaller('row -> 'row, WWH, new InnerJoin(), reducers)
      .map(('vec_wa, 'vec_wwh, 'vec, 'denom_vec_list) -> 'vec_mult){x : (RealVector, RealVector, RealVector, List[RealVector]) =>
        val den_vec = x._4.tail.foldLeft(x._4.head.mapMultiply(x._4.head.dotProduct(x._3))){(a, b) => a.combineToSelf(1.0, b.dotProduct(x._3), b); a}
        val num = x._1.mapMultiply(1.0 + alpha)
        val den = x._2.add(den_vec.mapMultiply(alpha))
        num.ebeDivide(den)
      }
      .map('vec_mult -> 'vec_mult){x : RealVector => MatrixUtils.createRealVector(x.toArray.map{i => if(i.isInfinite || i.isNaN) 1.0 else i})}
      .project('row, 'vec_mult)

    // new H.
    val H_ = H.joinWithSmaller('row -> 'row, HM, new InnerJoin(), reducers)
      .map(('vec, 'bias, 'vec_mult) -> ('vec, 'bias)){x : (RealVector, Double, RealVector) => explodeHVector(createHVector(x._1, x._2).ebeMultiply(x._3))}
      .project('row, 'vec, 'bias)

    // -- Then update W.
    // HH'
    val HH = H_.mapTo(('vec, 'bias) -> 'HH){v : (RealVector, Double) =>
        val u = createHVector(v._1, v._2)
        u.outerProduct(u)
      }
      .groupAll{_.reduce[RealMatrix]('HH){(a, b) => a.add(b)}}

    // WHH'
    val WHH = W.crossWithTiny(HH)
      .map(('vec, 'bias) -> 'vec){x : (RealVector, Double) => createWVector(x._1, x._2)}
      .map(('HH, 'vec) -> 'vec_whh){x : (RealMatrix, RealVector) => x._1.operate(x._2)}
      .project('col, 'vec_whh, 'vec)

    // AH'
    val AH = H_.joinWithLarger('row -> 'row, A, new InnerJoin(), reducers)
      .map(('val, 'vec, 'bias) -> ('vec_ah, 'denom_vec)){x : (Double, RealVector, Double) =>
        val v = createHVector(x._2, x._3)
        (v.mapMultiply(x._1), v)
      }
      .groupBy('col){
        _.reduce[RealVector]('vec_ah){(a, b) => addTo(a, b)}
        .toList[RealVector]('denom_vec -> 'denom_vec_list)
        .reducers(reducers)
        //.forceToReducers
      }
      .project('col, 'vec_ah, 'denom_vec_list)

    // Pointwise multiplier to old W
    val WM = AH.joinWithSmaller('col -> 'col, WHH, new InnerJoin(), reducers)
      .map(('vec_ah, 'vec_whh, 'vec, 'denom_vec_list) -> 'vec_mult){x : (RealVector, RealVector, RealVector, List[RealVector]) =>
        val den_vec = x._4.tail.foldLeft(x._4.head.mapMultiply(x._4.head.dotProduct(x._3))){(a, b) => a.combineToSelf(1.0, b.dotProduct(x._3), b); a}
        val num = x._1.mapMultiply(1.0 + alpha)
        val den = x._2.add(den_vec.mapMultiply(alpha))
        x._1.ebeDivide(x._2)
      }
      .map('vec_mult -> 'vec_mult){x : RealVector => MatrixUtils.createRealVector(x.toArray.map{i => if(i.isInfinite || i.isNaN) 1.0 else i})}
      .project('col, 'vec_mult)

    // new W.
    val W_ = W.joinWithSmaller('col -> 'col, WM, new InnerJoin(), reducers)
      .map(('vec, 'bias, 'vec_mult) -> ('vec, 'bias)){x : (RealVector, Double, RealVector) => explodeWVector(createWVector(x._1, x._2).ebeMultiply(x._3))}
      .project('col, 'vec, 'bias)

    (H_, W_)
  }
}


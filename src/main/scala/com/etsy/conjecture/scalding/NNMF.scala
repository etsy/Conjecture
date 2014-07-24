package com.etsy.conjecture.scalding

import org.apache.commons.math3.linear._

import cascading.pipe.Pipe
import cascading.pipe.joiner.InnerJoin
import cascading.tuple.Fields

object NNMF extends Serializable {

  import com.twitter.scalding.Dsl._

  // based on http://research.microsoft.com/pubs/119077/dnmf.pdf

  // input:
  // A: a sparse matrix in the form ('row, 'col, 'val), with tuples of type (R, C, Double).
  // k: the dimension of the factorization.
  def initGaussian(A : Pipe, k : Int, reducers : Int = 500) : (Pipe, Pipe) = {
    val H0 = A.groupBy('row){_.size('count).reducers(reducers)}
      .map(() -> 'vec){_ : Unit => MatrixUtils.createRealVector((0 until k).map{i => math.random}.toArray)}
      .project('row, 'vec)
    val W0 = A.groupBy('col){_.size('count).reducers(reducers)}
      .map(() -> 'vec){_ : Unit => MatrixUtils.createRealVector((0 until k).map{i => math.random}.toArray)}
      .project('col, 'vec)
    (H0, W0)
  }

  // input:
  // A: a sparse matrix in the form ('row, 'col, 'val), with tuples of type (R, C, Double).
  // H: a dense matrix of ('row, 'vec)
  // W: a dense matrix of ('col, 'vec)
  // With W,H from initGaussian or a previous iteration.
  def updateGaussian(A : Pipe, H : Pipe, W : Pipe, reducers : Int = 500) : (Pipe, Pipe) = {

    // Note that row and column vectors are both represented as a RealVector which doesnt have an orientation.
    // Therefore whether it is a row or column will have to be inferred from context.

    // -- First update H.
    // W'W
    val WW = W.mapTo('vec -> 'WW){v : RealVector => v.outerProduct(v)}
      .groupAll{_.reduce[RealMatrix]('WW){(a, b) => a.add(b)}}

    // W'WH
    val WWH = H.crossWithTiny(WW)
      .map(('WW, 'vec) -> 'vec_wwh){x : (RealMatrix, RealVector) => x._1.operate(x._2)}
      .project('row, 'vec_wwh)

    // W'A
    val WA = W.joinWithLarger('col -> 'col, A, new InnerJoin(), reducers)
      .map(('val, 'vec) -> 'vec){x : (Double, RealVector) => x._2.mapMultiply(x._1)}
      .groupBy('row){_.reduce[RealVector]('vec -> 'vec_wa){(a, b) => a.add(b)}.reducers(reducers)}

    // Pointwise multiplier to old H
    val HM = WA.joinWithSmaller('row -> 'row, WWH, new InnerJoin(), reducers)
      .map(('vec_wa, 'vec_wwh) -> 'vec_mult){x : (RealVector, RealVector) => x._1.ebeDivide(x._2)}
      .project('row, 'vec_mult)

    // new H.
    val H_ = H.joinWithSmaller('row -> 'row, HM, new InnerJoin(), reducers)
      .map(('vec, 'vec_mult) -> 'vec){x : (RealVector, RealVector) => x._1.ebeMultiply(x._2)}
      .project('row, 'vec)

    // -- Then update W.
    // HH'
    val HH = H_.mapTo('vec -> 'HH){v : RealVector => v.outerProduct(v)}
      .groupAll{_.reduce[RealMatrix]('HH){(a, b) => a.add(b)}}

    // WHH'
    val WHH = W.crossWithTiny(HH)
      .map(('HH, 'vec) -> 'vec_whh){x : (RealMatrix, RealVector) => x._1.operate(x._2)}
      .project('col, 'vec_whh)

    // AH'
    val AH = H_.joinWithLarger('row -> 'row, A, new InnerJoin(), reducers)
      .map(('val, 'vec) -> 'vec){x : (Double, RealVector) => x._2.mapMultiply(x._1)}
      .groupBy('col){_.reduce[RealVector]('vec -> 'vec_ah){(a, b) => a.add(b)}.reducers(reducers)}

    // Pointwise multiplier to old W
    val WM = AH.joinWithSmaller('col -> 'col, WHH, new InnerJoin(), reducers)
      .map(('vec_ah, 'vec_whh) -> 'vec_mult){x : (RealVector, RealVector) => x._1.ebeDivide(x._2)}
      .project('col, 'vec_mult)

    // new W.
    val W_ = W.joinWithSmaller('col -> 'col, WM, new InnerJoin(), reducers)
      .map(('vec, 'vec_mult) -> 'vec){x : (RealVector, RealVector) => x._1.ebeMultiply(x._2)}
      .project('col, 'vec)

    (H_, W_)
  }
}


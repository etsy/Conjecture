package com.etsy.conjecture.scalding

import org.apache.commons.math3.linear._
import cascading.pipe.Pipe
import cascading.pipe.joiner.InnerJoin
import cascading.tuple.Fields
import scala.util.Random

object SVD extends Serializable {

  import com.twitter.scalding.Dsl._

  /**
  * based on http://amath.colorado.edu/faculty/martinss/Pubs/2012_halko_dissertation.pdf
  * page 121.
  *
  * generic parameters:
  * R: the type of the row name variable.
  * C: the type of the column name variable.
  *
  * input:
  * X: a sparse matrix in the form ('row, 'col, 'val), with tuples of type (R, C, Double).
  * d: number of principle components / singular values to compute
  * extra_power: whether to take the second power of XX' in order to improve the approximation quality.
  * reducers: how many reducers to use in the map-reduce stages.
  *
  * output:
  * (U, E, V) with
  * U : pipe of ('row, 'vec) where vec is a RealVector
  * E : pipe of 'E which is an Array[Double] of singular values.
  * V : pipe of ('col, 'vec) where vec is a RealVector
  * note that the vectors are rows of the matrices U and V, not the columns which correspond to the left and right singular vectors.
  */
  def apply[R, C](X : Pipe, d : Int, extra_power : Boolean = true, reducers : Int = 500) : (Pipe, Pipe, Pipe) = {

    // Sample the columns, into the thin matrix.
    val XS = X.groupBy('row){_.toList[(C, Double)](('col, 'val) -> 'list).reducers(reducers)}
      .map('list -> 'vec){l : List[(C, Double)] =>
        val a = new Array[Double](d+10)
        l.foreach{i =>
          val r = new Random(i._1.hashCode.toLong)
          (0 until (d+10)).foreach{j =>
            a(j) += r.nextGaussian * i._2
          }
        }
        MatrixUtils.createRealVector(a)
      }
      .project('row, 'vec)

    // Multiply by powers of XX'.  This improves the approximation quality.
    val XXXS = X
      .joinWithSmaller('row -> 'row_, XS.rename('row -> 'row_), new InnerJoin(), reducers)
      .map(('val, 'vec) -> 'vec){x : (Double, RealVector) => x._2.mapMultiply(x._1)}
      .groupBy('col){_.reduce('vec -> 'vec){(a : RealVector, b : RealVector) => a.add(b)}.forceToReducers.reducers(reducers)}
      .joinWithSmaller('col -> 'col_, X.rename('col -> 'col_), new InnerJoin(), reducers)
      .map(('val, 'vec) -> 'vec){x : (Double, RealVector) => x._2.mapMultiply(x._1)}
      .groupBy('row){_.reduce('vec -> 'vec2){(a : RealVector, b : RealVector) => a.add(b)}.forceToReducers.reducers(reducers)}

    val Y = (if(extra_power) {
      val XXXXXS = X
        .joinWithSmaller('row -> 'row_, XXXS.rename('row -> 'row_), new InnerJoin(), reducers)
        .map(('val, 'vec2) -> 'vec2){x : (Double, RealVector) => x._2.mapMultiply(x._1)}
        .groupBy('col){_.reduce('vec2 -> 'vec2){(a : RealVector, b : RealVector) => a.add(b)}.forceToReducers.reducers(reducers)}
        .joinWithSmaller('col -> 'col_, X.rename('col -> 'col_), new InnerJoin(), reducers)
        .map(('val, 'vec2) -> 'vec2){x : (Double, RealVector) => x._2.mapMultiply(x._1)}
        .groupBy('row){_.reduce('vec2 -> 'vec2){(a : RealVector, b : RealVector) => a.add(b)}.forceToReducers.reducers(reducers)}

      XS
        .joinWithSmaller('row -> 'row, XXXS, new InnerJoin(), reducers)
        .map(('vec, 'vec2) -> 'vec){x : (RealVector, RealVector) => x._1.append(x._2)}
        .project('row, 'vec)
        .joinWithSmaller('row -> 'row, XXXXXS, new InnerJoin(), reducers)
        .map(('vec, 'vec2) -> 'vec){x : (RealVector, RealVector) => x._1.append(x._2)}
        .project('row, 'vec)
    } else {
      XS
        .joinWithSmaller('row -> 'row, XXXS, new InnerJoin(), reducers)
        .map(('vec, 'vec2) -> 'vec){x : (RealVector, RealVector) => x._1.append(x._2)}
        .project('row, 'vec)
    })

    // What follows is a QR decomposition of Y.
    // Note: Y = QR means Y'Y = R'R so R = chol(Y'Y)
    val YY = Y.mapTo('vec -> 'mat){x : RealVector => x.outerProduct(x)}
      .groupAll{_.reduce('mat -> 'mat){(a : RealMatrix, b : RealMatrix) => a.add(b)}}
      .mapTo('mat -> 'mat){m : RealMatrix =>
        val chol = new CholeskyDecomposition(m)
        new LUDecomposition(chol.getL).getSolver.getInverse
      }

    // Determine Q = YR^{-1}
    val Q = Y.crossWithTiny(YY)
      .map(('vec, 'mat) -> 'vec){x : (RealVector, RealMatrix) => x._2.operate(x._1)}
      .project('row, 'vec)

    // B = Q'X
    val B = X.joinWithSmaller('row -> 'row, Q, new InnerJoin(), reducers)
      .map(('val, 'vec) -> 'vec){x : (Double, RealVector) => x._2.mapMultiply(x._1)}
      .groupBy('col){_.reduce('vec -> 'vec){(a : RealVector, b : RealVector) => a.add(b)}.reducers(reducers).forceToReducers}

    val EB = B.mapTo('vec -> 'mat){x : RealVector => x.outerProduct(x)}
      .groupAll{_.reduce('mat -> 'mat){(a : RealMatrix, b : RealMatrix) => a.add(b)}}
      .mapTo('mat -> ('eigs, 'eigmat, 'orthomat)){m : RealMatrix =>
        val e = new EigenDecomposition(m)
        (e.getRealEigenvalues,
         e.getVT,
         e.getVT.multiply(MatrixUtils.createRealDiagonalMatrix(e.getRealEigenvalues.map{v => if(v < 0.00000001) 0.0 else 1.0 / math.sqrt(v)})))
      }

    val E = EB.project('eigs).map('eigs -> 'eigs){x : Array[Double] => (0 until d).map{i => math.sqrt(x(i))}.toArray}

    val U = Q.crossWithTiny(EB.project('eigmat))
      .map(('vec, 'eigmat) -> 'vec){x : (RealVector, RealMatrix) => x._2.operate(x._1).getSubVector(0,d)}
      .project('row, 'vec)

    val V = B.crossWithTiny(EB.project('orthomat))
      .map(('vec, 'orthomat) -> 'vec){x : (RealVector, RealMatrix) => x._2.operate(x._1).getSubVector(0,d)}
      .project('col, 'vec)

    (U, E, V)
  }
}


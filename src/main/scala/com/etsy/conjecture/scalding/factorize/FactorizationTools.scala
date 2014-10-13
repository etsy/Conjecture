package com.etsy.conjecture.scalding.factorize

import cascading.pipe.Pipe
import org.apache.commons.math3.linear._

object FactorizationTools {

  def approxLeftFactorsLeastSquares(rightFactors : Pipe, id_sym : Symbol, right_vec_sym : Symbol,
                                    designMatrix : Pipe, left_id : Symbol, right_id : Symbol,
                                    left_vec_symbol : Symbol)
                                   (spill_threshold : Int = 1000000, parallelism : Int = 1000) : Pipe = {

    import com.twitter.scalding.Dsl._
    val inv_sym = Symbol(right_vec_sym.name + "_inverse")

    val inv_self_outer = rightFactors
      .mapTo(right_vec_sym -> right_vec_sym) {
        l : RealVector => l.outerProduct(l)
      }
      .groupAll{ _.reduce[RealMatrix](right_vec_sym){ (acc, x) => acc.add(x) } }
      .mapTo(right_vec_sym -> inv_sym) {
        ll : RealMatrix => new LUDecomposition(ll).getSolver.getInverse
      }

    val weighted_right_factors = rightFactors
      .crossWithTiny(inv_self_outer)
      .map((right_vec_sym, inv_sym) -> right_vec_sym) {
        x:(RealVector, RealMatrix) => x._2.operate(x._1)
      }
      .project(id_sym, right_vec_sym)


    val left_vectors = designMatrix.joinWithSmaller(right_id -> id_sym, weighted_right_factors)
      .groupBy(left_id) {
        _.reduce[RealVector](right_vec_sym -> left_vec_symbol){ (acc, x) => acc.add(x) }
         .reducers(parallelism)
         .spillThreshold(spill_threshold)
      }

      .project(left_id, left_vec_symbol)

    left_vectors
  }

}

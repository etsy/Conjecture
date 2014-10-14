package com.etsy.conjecture.scalding.factorize

import cascading.pipe.Pipe
import org.apache.commons.math3.linear._



object FactorizationTools {

  def approxLeftFactorsLeastSquares(rightFactors : Pipe, id_sym : Symbol, right_vec_sym : Symbol,
                                    designMatrix : Pipe, left_id : Symbol, right_id : Symbol,
                                    left_vec_symbol : Symbol,
                                    spill_threshold : Int = 1000000, parallelism : Int = 1000) : Pipe = {

    import com.twitter.scalding.Dsl._
    approxWeightLeftFactorsLeastSquares(rightFactors, id_sym, right_vec_sym,
                                        designMatrix.insert('weight, 1.0), left_id, right_id,
                                        'weight, left_vec_symbol, spill_threshold, parallelism)
  }

  def approxWeightLeftFactorsLeastSquares(rightFactors : Pipe, id_sym : Symbol, right_vec_sym : Symbol,
                                          designMatrix : Pipe, left_id : Symbol, right_id : Symbol,
                                          value_sym : Symbol, left_vec_symbol : Symbol,
                                          spill_threshold : Int = 1000000, parallelism : Int = 1000) : Pipe = {

    import com.twitter.scalding.Dsl._
    val inv_sym = 'inverse

    val inv_self_outer = rightFactors
      .mapTo(right_vec_sym -> right_vec_sym) {
        l : RealVector => l.outerProduct(l)
      }
      .groupAll{ _.reduce[RealMatrix](right_vec_sym){ (x, y) => x.add(y) } }
      .mapTo(right_vec_sym -> inv_sym) {
        ll : RealMatrix => new LUDecomposition(ll).getSolver.getInverse
      }

    val premultiplied_right_factors = rightFactors
      .crossWithTiny(inv_self_outer)
      .map((right_vec_sym, inv_sym) -> right_vec_sym) {
        x:(RealVector, RealMatrix) => x._2.operate(x._1)
      }
      .project(id_sym, right_vec_sym)


    designMatrix.joinWithSmaller(right_id -> id_sym, premultiplied_right_factors)
      .map((right_vec_sym, value_sym) -> right_vec_sym) { x : (RealVector, Double) => x._1.mapMultiply(x._2) }
      .groupBy(left_id) {
        _.reduce[RealVector](right_vec_sym -> left_vec_symbol){ (x, y) => x.add(y) }
         .reducers(parallelism)
         .spillThreshold(spill_threshold)
      }
      .project(left_id, left_vec_symbol)
  }

}

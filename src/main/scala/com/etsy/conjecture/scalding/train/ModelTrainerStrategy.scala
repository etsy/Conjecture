package com.etsy.conjecture.scalding.train

import cascading.pipe.Pipe

import com.etsy.conjecture.data._
import com.etsy.conjecture.model._

import com.twitter.scalding._

trait ModelTrainerStrategy[L <: Label, M <: UpdateableModel[L, M]] extends Serializable {
    import Dsl._

    // How many iterations of training to perform.
    def getIters: Int = 1

    // The subclass just implements a thing that creates an initial model, from a set of
    // initial parameters.
    def getModel: M

    // Optionally subsample depending on the lable.
    def sampleProb(label: L): Double = 1.0

    // Optionally perform some post-processing on the model after training.
    def modelPostProcess(m: M): M = m

    def miniBatchSize: Int = 1

    // Function to merge two sub-models.
    // Can be overriden to change default behavior.
    def mergeModels(model1: M, model2: M, iteration1: Int, iteration2: Int): M = {
        model1.merge(model2, 1.0)
        model1
    }

    // Do something at the end of the iteration.
    def endIteration(model: M, iteration: Int, models: Int): M = {
        model.reScale(1.0 / models)
        model
    }

    // Train the model on a mini batch.
    // Returns the trained model, remining instances from the list, and the iteration number.
    def updateModelOnMiniBatch(model: M, instances: List[LabeledInstance[L]], start_iteration: Int): (M, List[LabeledInstance[L]], Int) = {
        val batch_sz = miniBatchSize // might be something that needs computing who knows.   
        val mini_batch = new java.util.ArrayList[LabeledInstance[L]](miniBatchSize)
        val n_batches = instances.size / batch_sz
        var batch = 0
        val iterator = instances.iterator
        while (batch < n_batches) { // one extra iteration to get the remainder into the array.
            (0 until batch_sz).foreach { i => mini_batch.add(i, iterator.next) }
            model.update(mini_batch)
            batch += 1
        }
        val remainder = iterator.toList
        (model, remainder, start_iteration + n_batches)
    }

    // This implements the associative operation of the model training.
    // Note that each tuple contains an instance not yet used for training.
    def modelReduceFunction(a: (M, List[LabeledInstance[L]], Int, Int), b: (M, List[LabeledInstance[L]], Int, Int)): (M, List[LabeledInstance[L]], Int, Int) = {
        if (a._3 > 0 && b._3 > 0) {
            // Both models have some prior training.
            val ua = updateModelOnMiniBatch(a._1, a._2, a._3)
            val ub = updateModelOnMiniBatch(b._1, b._2, b._3)
            // Merge together
            (mergeModels(ua._1, ub._1, ua._3, ub._3), ua._2 ++ ub._2, ua._3 + ub._3, a._4 + b._4)
        } else if (b._3 > 0) {
            // Only model b has some prior training.
            // Update model b using all instances.
            val uba = updateModelOnMiniBatch(b._1, a._2 ++ b._2, b._3)
            (uba._1, uba._2, uba._3, b._4)
        } else {
            // Either no model is trained, or only a is.
            // Update model a using whatever intances are available.
            val uab = updateModelOnMiniBatch(a._1, a._2 ++ b._2, a._3)
            (uab._1, uab._2, uab._3, a._4)
        }
    }
}

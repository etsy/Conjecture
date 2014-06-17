package com.etsy.conjecture.scalding.train

import cascading.pipe.Pipe

import com.etsy.conjecture.data._
import com.etsy.conjecture.model._

import com.twitter.scalding._

class SmallModelTrainer[L <: Label, M <: UpdateableModel[L, M]](strategy: ModelTrainerStrategy[L, M]) extends AbstractModelTrainer[L, M] {
    import Dsl._

    // Functionality to train a small model (hundreds of thousands of features, arbitrarily many instances)
    // Trains a model on each mapper, then aggregates them on one reducer.
    // The last step is expensive if the dimensionality is great, since the reducer has to deserialize large StringKeyedVectors.
    def train(instances: Pipe, instanceField: Symbol = 'instance, modelField: Symbol = 'mode): Pipe = {
        // Begin training.
        trainRecursively(None, modelField, instances, instanceField, strategy.getIters)
    }

    // Additional training for a small model.
    def reTrain(instances: Pipe, instanceField: Symbol, model: Pipe, modelField: Symbol): Pipe = {
        // Begin training.
        trainRecursively(Some(model), modelField, instances, instanceField, strategy.getIters)
    }

    // This implements a full iteration of training, ending with a pipe with a model.
    protected def trainIteration(modelPipe: Option[Pipe], modelField: Symbol, instancePipe: Pipe, instanceField: Symbol): Pipe = {
        val iterationField: Symbol = '__iteration__
        val modelCountField: Symbol = '__model_count__
        // Subsample instances.
        val subsampled = instancePipe.filter(instanceField) { i: LabeledInstance[L] => math.random < strategy.sampleProb(i.getLabel) }
        // Get models on each mapper.
        (modelPipe match {
            case Some(pipe) => subsampled.project(instanceField).crossWithTiny(pipe.project(modelField))
            case _ => subsampled.mapTo(instanceField -> (instanceField, modelField)) { x: LabeledInstance[L] => (x, strategy.getModel) }
        })
            // Count iteration numbers.
            .insert(iterationField, 0)
            .insert(modelCountField, 1)
            // Convert instances to instance list.
            .map(instanceField -> instanceField) { i: LabeledInstance[L] => List(i) }
            // Perform map-side aggregation of models, which are then sent to a single reduce node for merging.
            .groupAll {
                _.reduce[(M, List[LabeledInstance[L]], Int, Int)](
                    (modelField, instanceField, iterationField, modelCountField) -> (modelField, instanceField, iterationField, modelCountField))(strategy.modelReduceFunction)
            }
            .mapTo((modelField, iterationField, modelCountField) -> modelField) { x: (M, Int, Int) => strategy.endIteration(x._1, x._2, x._3) }
    }

    protected def trainRecursively(modelPipe: Option[Pipe], modelField: Symbol, instancePipe: Pipe, instanceField: Symbol, iterations: Int): Pipe = {
        val updatedPipe = trainIteration(modelPipe, modelField, instancePipe, instanceField)
        if (iterations == 1) {
            updatedPipe.map(modelField -> modelField) { strategy.modelPostProcess }
        } else {
            trainRecursively(Some(updatedPipe), modelField, instancePipe, instanceField, iterations - 1)
        }
    }

}

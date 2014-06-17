package com.etsy.conjecture.scalding.train

import cascading.pipe.Pipe
import com.twitter.scalding._
import com.etsy.conjecture.data._
import com.etsy.conjecture.model._

class MulticlassModelTrainer(args: Args, categories: Array[String]) extends AbstractModelTrainer[MulticlassLabel, MulticlassLogisticRegression] with ModelTrainerStrategy[MulticlassLabel, MulticlassLogisticRegression] {

    val iters = args.getOrElse("iters", "1").toInt

    override def getIters: Int = iters

    def getModel: MulticlassLogisticRegression = {
        new MulticlassLogisticRegression(categories)
    }

    val bins = args.getOrElse("bins", "100").toInt

    val trainer = if (args.boolean("large")) new LargeModelTrainer(this, bins) else new SmallModelTrainer(this)

    // Size of minibatch for mini-batch training, defaults to 1 which is just SGD.
    val batchsz = args.getOrElse("mini_batch_size", "1").toInt
    override def miniBatchSize: Int = batchsz

    def train(instances: Pipe, instanceField: Symbol = 'instance, modelField: Symbol = 'model): Pipe = {
        trainer.train(instances, instanceField, modelField)
    }

    def reTrain(instances: Pipe, instanceField: Symbol, model: Pipe, modelField: Symbol): Pipe = {
        trainer.reTrain(instances, instanceField, model, modelField)
    }
}

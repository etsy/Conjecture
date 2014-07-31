package com.etsy.conjecture.scalding.train

import cascading.pipe.Pipe
import com.twitter.scalding._
import com.etsy.conjecture.data._
import com.etsy.conjecture.model._

class RegressionModelTrainer(args: Args) extends AbstractModelTrainer[RealValuedLabel, UpdateableLinearModel[RealValuedLabel]]
    with ModelTrainerStrategy[RealValuedLabel, UpdateableLinearModel[RealValuedLabel]] {

    // number of iterations for
    // sequential gradient descent
    val iters = args.getOrElse("iters", "1").toInt

    override def getIters: Int = iters

    // What type of learning rate schedule?
    // options are:
    // 1. exponential
    // 2. adagrad
    val learningRateSchedule = args.getOrElse("learning_rate_schedule", "exponential").toString

    // weight on laplace regularization- a laplace prior on the parameters
    // sparsity inducing ala lasso
    val laplace = args.getOrElse("laplace", "0.5").toDouble

    // weight on gaussian prior on the parameters
    // similar to ridge 
    val gauss = args.getOrElse("gauss", "0.5").toDouble

    val modelType = "least_squares" // just one model type for regression at the moment

    // initial learning rate used for SGD learning. this decays according to the
    // inverse of the epoch
    val initialLearningRate = args.getOrElse("rate", "0.1").toDouble

    // Base of the exponential learning rate (e.g., 0.99^{# examples seen}).
    val exponentialLearningRateBase = args.getOrElse("exponential_learning_rate_base", "1.0").toDouble

    // Whether to use the exponential learning rate.  If not chosen then the learning rate is like 1.0 / epoch.
    val useExponentialLearningRate = args.boolean("exponential_learning_rate_base")

    // A fudge factor so that an "epoch" for the purpose of learning rate computation can be more than one example,
    // in which case the "epoch" will take a fractional amount equal to {# examples seen} / examples_per_epoch.
    val examplesPerEpoch = args.getOrElse("examples_per_epoch", "10000").toDouble

    /**
     *  Learning rate schedule for regularization
     */
    val regularizerExponentialLearningRate = new ExponentialLearningRate().setExamplesPerEpoch(examplesPerEpoch)
                                             .setUseExponentialLearningRate(useExponentialLearningRate)
                                             .setExponentialLearningRateBase(exponentialLearningRateBase)
                                             .setInitialLearningRate(initialLearningRate)


    val rateComputer = learningRateSchedule match {
        case "exponential" => new ExponentialLearningRate().setExamplesPerEpoch(examplesPerEpoch)
                                                           .setUseExponentialLearningRate(useExponentialLearningRate)
                                                           .setExponentialLearningRateBase(exponentialLearningRateBase)
                                                           .setInitialLearningRate(initialLearningRate)
        case "adagrad" => new Adagrad().setInitialLearningRate(initialLearningRate)
    }

    val regularizer = new RegularizationUpdater(regularizerExponentialLearningRate, gauss, laplace);

    def getModel: UpdateableLinearModel[RealValuedLabel] = {
        val model = modelType match {
            case "least_squares" => new LeastSquaresRegressionModel(rateComputer, regularizer)
        }
        model
    }

    val bins = args.getOrElse("bins", "100").toInt

    val trainer = if (args.boolean("large")) new LargeModelTrainer(this, bins) else new SmallModelTrainer(this)

    def train(instances: Pipe, instanceField: Symbol = 'instance, modelField: Symbol = 'model): Pipe = {
        trainer.train(instances, instanceField, modelField)
    }

    def reTrain(instances: Pipe, instanceField: Symbol, model: Pipe, modelField: Symbol): Pipe = {
        trainer.reTrain(instances, instanceField, model, modelField)
    }
}

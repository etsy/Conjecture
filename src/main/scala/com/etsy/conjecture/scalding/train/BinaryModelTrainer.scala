package com.etsy.conjecture.scalding.train

import cascading.pipe.Pipe
import com.twitter.scalding._
import com.etsy.conjecture.data._
import com.etsy.conjecture.model._

import java.io.File
import scala.io.Source

class BinaryModelTrainer(args: Args) extends AbstractModelTrainer[BinaryLabel, UpdateableLinearModel[BinaryLabel]] with ModelTrainerStrategy[BinaryLabel, UpdateableLinearModel[BinaryLabel]] {

    // Number of iterations for
    // sequential gradient descent
    var iters = args.getOrElse("iters", "1").toInt

    override def getIters: Int = iters

    // What type of linear model should be used?
    // options are:
    // 1. perceptron
    // 2. passive_aggressive
    // 3. logistic_regression
    // 4. mira
    val modelType = args.getOrElse("model", "passive_aggressive").toString

    // aggressiveness parameter for passive aggressive classifier
    val aggressiveness = args.getOrElse("aggressiveness", "2.").toDouble

    // What type of learning rate schedule?
    // options are:
    // 1. exponential
    // 2. adagrad
    val learningRateSchedule = args.getOrElse("learning_rate_schedule", "exponential").toString

    val finalThresholding = args.getOrElse("final_thresholding", "0.0").toDouble

    // Initial learning rate used for SGD learning. this decays according to the
    // inverse of the epoch
    val initialLearningRate = args.getOrElse("rate", "0.1").toDouble

    // Base of the exponential learning rate (e.g., 0.99^{# examples seen}).
    val exponentialLearningRateBase = args.getOrElse("exponential_learning_rate_base", "1.0").toDouble

    // Whether to use the exponential learning rate.  If not chosen then the learning rate is like 1.0 / epoch.
    val useExponentialLearningRate = args.boolean("exponential_learning_rate_base")

    // A fudge factor so that an "epoch" for the purpose of learning rate computation can be more than one example,
    // in which case the "epoch" will take a fractional amount equal to {# examples seen} / examples_per_epoch.
    val examplesPerEpoch = args.getOrElse("examples_per_epoch", "10000").toDouble

    // How to subsample each class, in the case of imbalanced data.
    val zeroClassProb = args.getOrElse("zero_class_prob", "1.0").toDouble
    val oneClassProb = args.getOrElse("one_class_prob", "1.0").toDouble

    /**
     *  Learning rate schedule for stochastic gradient descent classifiers
     */
    val rateComputer = learningRateSchedule match {
        case "exponential" => new ExponentialLearningRate().setExamplesPerEpoch(examplesPerEpoch)
                                                           .setUseExponentialLearningRate(useExponentialLearningRate)
                                                           .setExponentialLearningRateBase(exponentialLearningRateBase)
                                                           .setInitialLearningRate(initialLearningRate)
        case "adagrad" => new Adagrad().setInitialLearningRate(initialLearningRate)
    }

    // Weight on laplace regularization- a laplace prior on the parameters
    // sparsity inducing ala lasso
    val laplace = args.getOrElse("laplace", "0.0").toDouble

    // Weight on gaussian prior on the parameters
    // similar to ridge 
    val gauss = args.getOrElse("gauss", "0.0").toDouble

    // What type of regularization learning rate schedule?
    // options are:
    // 1. exponential
    // 2. constant
    val regularizationLearningRateSchedule = args.getOrElse("reg_learning_rate_schedule", "exponential").toString

    // Initial learning rate used for SGD learning. This decays according to the
    // inverse of the epoch
    val regInitialLearningRate = args.getOrElse("reg_rate", "0.1").toDouble

    // Base of the exponential learning rate (e.g., 0.99^{# examples seen}).
    val regExponentialLearningRateBase = args.getOrElse("reg_exponential_learning_rate_base", "1.0").toDouble

    // Whether to use the exponential learning rate.  If not chosen then the learning rate is like 1.0 / epoch.
    val useRegExponentialLearningRate = args.boolean("reg_exponential_learning_rate_base")

    /**
     *  Learning rate schedule for regularization
     */
    val regularizerExponentialLearningRate = regularizationLearningRateSchedule match {
        case "exponential" => new ExponentialLearningRate().setExamplesPerEpoch(examplesPerEpoch)
                                                           .setUseExponentialLearningRate(useRegExponentialLearningRate)
                                                           .setExponentialLearningRateBase(regExponentialLearningRateBase)
                                                           .setInitialLearningRate(regInitialLearningRate)
        case "constant" => new ConstantLearningRate().setInitialLearningRate(regInitialLearningRate)
    }

    val regularizer = new RegularizationUpdater(regularizerExponentialLearningRate, gauss, laplace);

    // period of gradient truncation updates
    val truncationPeriod = args.getOrElse("period", Int.MaxValue.toString).toInt

    // aggressiveness of gradient truncation updates, how much shrinkage
    // is applied to the model's parameters
    val truncationAlpha = args.getOrElse("alpha", "0.0").toDouble

    // threshold for applying gradient truncation updates
    // parameter values smaller than this in magnitude are truncated
    val truncationThresh = args.getOrElse("thresh", "0.0").toDouble


    // What type of truncation learning rate schedule?
    // options are:
    // 1. exponential
    // 2. constant
    val truncationLearningRateSchedule = args.getOrElse("trunc_learning_rate_schedule", "exponential").toString

    // initial learning rate used for SGD learning. this decays according to the
    // inverse of the epoch
    val truncInitialLearningRate = args.getOrElse("trunc_rate", "0.1").toDouble

    // Base of the exponential learning rate (e.g., 0.99^{# examples seen}).
    val truncExponentialLearningRateBase = args.getOrElse("trunc_exponential_learning_rate_base", "1.0").toDouble

    // Whether to use the exponential learning rate.  If not chosen then the learning rate is like 1.0 / epoch.
    val useTruncExponentialLearningRate = args.boolean("trunc_exponential_learning_rate_base")

    /**
     *  Learning rate schedule for truncation
     */
    val truncationExponentialLearningRate = truncationLearningRateSchedule match {
        case "exponential" => new ExponentialLearningRate().setExamplesPerEpoch(examplesPerEpoch)
                                                           .setUseExponentialLearningRate(useTruncExponentialLearningRate)
                                                           .setExponentialLearningRateBase(truncExponentialLearningRateBase)
                                                           .setInitialLearningRate(truncInitialLearningRate)
        case "constant" => new ConstantLearningRate().setInitialLearningRate(truncInitialLearningRate)
    }


    // Size of minibatch for mini-batch training, defaults to 1 which is just SGD.
    val batchsz = args.getOrElse("mini_batch_size", "1").toInt
    override def miniBatchSize: Int = batchsz

    override def sampleProb(l: BinaryLabel): Double = {
        if (l.getValue < 0.5)
            zeroClassProb
        else
            oneClassProb
    }

    override def modelPostProcess(m: UpdateableLinearModel[BinaryLabel]): UpdateableLinearModel[BinaryLabel] = {
        m.thresholdParameters(finalThresholding)
        m.setArgString(args.toString)
        m
    }

    def getModel: UpdateableLinearModel[BinaryLabel] = {
        val model = modelType match {
            case "perceptron" => new Perceptron(rateComputer, regularizer)
            case "passive_aggressive" => new PassiveAggressive().setC(aggressiveness)
            case "logistic_regression" => new LogisticRegression(rateComputer, regularizer)
            case "mira" => new MIRA()
        }
        model.setTruncationPeriod(truncationPeriod)
            .setTruncationThreshold(truncationThresh)
            .setTruncationUpdate(truncationAlpha)
            .setTruncationLearningRate(truncationExponentialLearningRate)
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

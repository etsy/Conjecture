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

    // weight on laplace regularization- a laplace prior on the parameters
    // sparsity inducing ala lasso
    val laplace = args.getOrElse("laplace", "0.5").toDouble

    // weight on gaussian prior on the parameters
    // similar to ridge 
    val gauss = args.getOrElse("gauss", "0.5").toDouble

    val modelType = "least_squares" // just one model type for regression at the moment

    def getModel: UpdateableLinearModel[RealValuedLabel] = {
        val model = modelType match {
            case "least_squares" => new LeastSquaresRegressionModel()
        }
        model.setLaplaceRegularizationWeight(laplace)
            .setGaussianRegularizationWeight(gauss)
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

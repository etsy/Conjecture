package com.etsy.conjecture.scalding.util

import java.io.Serializable

import cascading.pipe.Pipe
import com.etsy.conjecture.data._
import com.etsy.conjecture.evaluation._
import com.etsy.conjecture.model._
import com.etsy.conjecture.scalding.evaluate.{BinaryEvaluator, GenericEvaluator, MulticlassEvaluator, RegressionEvaluator}
import com.etsy.conjecture.scalding.train._
import com.twitter.scalding.Dsl._
import com.twitter.scalding._

import scala.util.Random

/**
  * Samples random parameter values to perform a fast efficient hyperparameter search
  */
abstract class HyperparameterSearcher[L <: Label, M <: UpdateableModel[L, M], E <: ModelEvaluation[L]]
                                      (val options: DynamicOptions, val parameters: Seq[HyperParameter[_]],
                                       val numTrials: Int, rng: Random = new Random(0)) extends Serializable {

  //Map of trial id to a randomly sampled set of parameters
  val settings = (0 until numTrials).map { trial : Int => trial -> sampledParameters(rng) }.toMap

  def getModelTrainer(args: Args): ModelTrainerStrategy[L, M]
  val evaluator: GenericEvaluator[L]

  //draw parameter values from given sample method
  //save values in a new Arg instance
  def sampledParameters(rng: Random): Args = {
    parameters.foreach(_.set(rng))
    options.unParse
  }

  def search (instances: Pipe, instance_field: Symbol): (Pipe, Pipe) = {
    //Split train test by ratio
    //TODO Should make this into a parameter
    val splitSet = instances.map(instance_field -> '__fold) { li: LabeledInstance[L] => rng.nextInt(10) <= 7 }
    val trainSet = splitSet.filter('__fold) { foldId: Boolean => foldId }
    val testSet = splitSet.filter('__fold) { foldId: Boolean => !foldId }

    //Restructure data pipes into trainable format and assign an ID that defines the random setting to use
    val train = generateTrials(trainSet, instance_field)
    val test = generateTrials(testSet, instance_field)

    val models = trainTrials(train)

    val rawResults = evaluate(models, test)

    val runResults = rawResults
      .mapTo(('settings, 'eval) -> 'result) {
        x: (Args, Double) =>
          x._2 + "\t" + x._1.toString
      }
    //Tally up test accs to find metrics on tested param values
    val paramReport = createParameterReport(rawResults)

    (runResults, paramReport)
  }

  def generateTrials(instances: Pipe, instance_field: Symbol): Pipe = {
    instances
      .flatMapTo('instance -> ('instance, 'trial)) {
         instance: LabeledInstance[L] =>
             settings.keySet.map {
               trial: Int =>
                  (instance, trial)
             }
       }
       .groupBy('trial) {
          _.toList[LabeledInstance[L]]('instance -> 'instances).reducers(1000)
       }.project('trial, 'instances)
  }

  def trainTrials(instances: Pipe): Pipe = {
       instances
         .mapTo(('trial, 'instances) -> ('trial, 'model)) {
           x: (Int, List[LabeledInstance[L]]) =>
           //In the unlike case that a setting does not exist, use the default value
           val args: Args = settings.getOrElse(x._1, options.unParse)
           val instanceSet = x._2
           val modelTrainer = getModelTrainer(args)
           val model = modelTrainer.getModel
           //Train model
           instanceSet.foreach(model.update)
           (x._1, model)
        }
  }

  def evaluate (models: Pipe, testSet: Pipe): Pipe = {
     val eval = models
       .joinWithSmaller('trial -> 'trial, testSet)
       .mapTo(('model, 'instances, 'trial) -> ('eval, 'settings)) {
          x: (M, List[LabeledInstance[L]], Int) =>
          val model = x._1
          val testList = x._2
          val args = settings.getOrElse(x._3, options.unParse)
          val acc = evaluateAccuracy(testList, model)
          (acc, args)
       }
     eval
  }

  def evaluateAccuracy(instances: List[LabeledInstance[L]], model: M): Double = {
      val eval = evaluator.build
      instances.map {
        instance: LabeledInstance[L] =>
              val realLabel = instance.getLabel
              val prediction = model.predict(instance.getVector)
              eval.add(realLabel, prediction)
      }
      val agg = new EvaluationAggregator[L]()
      agg.add(eval)
      agg.getValue("Acc (avg)")
  }


  //Tally up evaluation scores of random parameter values and create a report of the mean/stdDev/max/count_of_runs
  def createParameterReport(rawResults: Pipe): Pipe = {
    rawResults
      .groupAll{
        _.toList[(Args, Double)](('settings, 'eval) -> 'results)
      }
      .mapTo('results -> 'report) {
         results: List[(Args,Double)] =>
           results.map{ x =>
             options.parse(x._1)
             parameters.foreach(_.accumulate(x._2))
            }
            parameters.map(_.report).mkString("\n")
      }
  }
}


class BinaryHyperparameterSearcher(option: DynamicOptions, parameters: Seq[HyperParameter[_]],
                                 numTrials: Int) extends HyperparameterSearcher[BinaryLabel, UpdateableLinearModel[BinaryLabel], BinaryModelEvaluation](option, parameters, numTrials) {
    val evaluator = new BinaryEvaluator()
    def getModelTrainer(args: Args) = new BinaryModelTrainer(args)
}

class RegressionHyperparameterSearcher(option: DynamicOptions, parameters: Seq[HyperParameter[_]],
                                 numTrials: Int) extends HyperparameterSearcher[RealValuedLabel, UpdateableLinearModel[RealValuedLabel], RegressionModelEvaluation](option, parameters, numTrials) {
    val evaluator = new RegressionEvaluator()
    def getModelTrainer(args: Args) = new RegressionModelTrainer(args)
}

class MulticlassHyperparameterSearcher(option: DynamicOptions, parameters: Seq[HyperParameter[_]], numTrials: Int, categories: Array[String]) extends HyperparameterSearcher[MulticlassLabel, UpdateableMulticlassLinearModel, MulticlassModelEvaluation](option, parameters, numTrials) {
    val evaluator = new MulticlassEvaluator(categories)
    def getModelTrainer(args: Args) = new MulticlassModelTrainer(args, categories)
}

/**
 * Sampling method for hyperparameters
 * Also defines how to bucket parameter values and accuracies for hyperparameter reports
 */
trait ParameterSampler[T] extends Serializable {
  def sample(rng: scala.util.Random): T
  //Array corresponds to bucketed parameter value, then the sum, sumSq, max, count of times that param bucket was run
  val buckets: Array[(T, Double, Double, Double, Int)]
  def valueToBucket(v: T): Int
  def accumulate(value: T, d: Double) {
    val v = math.max(0, math.min(valueToBucket(value), buckets.length-1))
    val (_, sum, sumSq, max, count) = this.buckets(v)
    this.buckets(v) = (value, sum+d, sumSq+d*d, math.max(max, d), count+1)
  }
}

/**
 * Samples uniformly one value from the sequence.
 */
class SampleFromSeq[T](seq: Seq[T]) extends ParameterSampler[T] {
  val buckets = seq.map(s => (s, 0.0, 0.0, 0.0, 0)).toArray
  def valueToBucket(v: T) = buckets.toSeq.map(_._1).indexOf(v)
  def sample(rng: Random) = seq(rng.nextInt(seq.length))
}

/**
 * Samples uniformly a double that falls within the range
 */
class UniformDoubleSampler(lower: Double, upper: Double, numBuckets: Int = 10) extends ParameterSampler[Double] {
  val dif = upper - lower
  val buckets = (0 to numBuckets).map(i => (0.0, 0.0, 0.0, 0.0, 0)).toArray
  def valueToBucket(d: Double) = (numBuckets*(d - lower)/dif).toInt
  def sample(rng: Random) = rng.nextDouble()*dif + lower
}
/**
 * Samples Doubles in the range such that their logarithm is uniform.
 * Useful for learning rates, variances, alphas, and other things which
 * vary in order of magnitude.
 */
class LogUniformDoubleSampler(lower: Double, upper: Double, numBuckets: Int = 10) extends ParameterSampler[Double] {
  val inner = new UniformDoubleSampler(math.log(lower), math.log(upper), numBuckets)
  def valueToBucket(v: Double) = inner.valueToBucket(math.log(v))
  val buckets = (0 to numBuckets).map(i => (0.0, 0.0, 0.0, 0.0, 0)).toArray
  def sample(rng: Random) = math.exp(inner.sample(rng))
}

/**
 * A container for a hyperparameter
 * @param option The DynamicOption wrapper for the parameter
 * @param sampler Sampler to use to return values for the parameter
 */
class HyperParameter[T](option: DynamicOption[T], val sampler: ParameterSampler[T]) {
  val buckets = sampler.buckets
  def set(rng: Random) { option.setValue(sampler.sample(rng)) }
  def accumulate(objective: Double) { sampler.accumulate(option.value, objective) }
  def report(): String = {
    val buff = new StringBuilder("Parameter: "+option.name+"\tMean\tStdDev\tMax\tCount\n")
    for ((value, sum, sumSq, max, count) <- buckets) {
      val mean = sum/count
      val stdDev = math.sqrt(sumSq/count - mean*mean)
      val metrics = value match {
        case v: Double => Vector(f"${v.toDouble}%2.15f",  f"$mean%2.4f", f"$stdDev%2.4f",f"$max%1.4f", count).mkString("\t")
        case _ => Vector(f"${value.toString}%20s",  f"$mean%2.4f", f"$stdDev%2.4f",f"$max%1.4f", count).mkString("\t")
      }
      buff.append(metrics + "\n")
    }
    buff.toString()
  }
}

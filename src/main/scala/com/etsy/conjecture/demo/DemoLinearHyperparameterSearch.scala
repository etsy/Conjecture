package com.etsy.scalding.jobs.conjecture

import scala.util.Random
import com.etsy.conjecture.scalding.util._
import com.etsy.conjecture.data._
import com.etsy.conjecture.model._
import com.twitter.scalding._
import cascading.tuple.Fields

/**
 * An example of a custom Job that would run the hyperparameter searcher for a Binary model
 * Takes command line arguments:
 * input: Path to training/testing data
 * out_dir: Path to output directory
 * num_trials: The number of models to train over random settings
 * model: Type of linear model to use for training
 */
class DemoLinearHyperparameterSearch(args : Args) extends BaseGridSearcher(args) {

  /*
   * Define the settings to be optimized as below.
   * DynamicOptions are generic type containers for Args that can perform various metric caluations.
   * All command line parameters given to this job are automatically added.
   * DynamicOption takes output name of param and the default value of the param.
   */
  class DefaultClassifierOptions extends DynamicOptions(args) {
    val laplace = new DynamicOption("laplace", 0.0)
    val gauss = new DynamicOption("gauss", 0.0)
    val rate = new DynamicOption("rate", 0.1)
    val numIters = new DynamicOption("iter", 5)
  }


  //Define your parameters to optimize
  val opts = new DefaultClassifierOptions

  //For each parameter you wish to optimize and defined about, create a hyperparameter instance with the dynamic container and the sampler type
  val parameters: Seq[HyperParameter[_]] = {
    val laplace = new HyperParameter(opts.laplace, new LogUniformDoubleSampler(1e-8, 1e-1))
    val gauss = new HyperParameter(opts.gauss, new LogUniformDoubleSampler(1e-8, 1e-1))
    val rate = new HyperParameter(opts.rate, new SampleFromSeq(List(.01, .001, .0001, .00001, .000001)))
    val iters = new HyperParameter(opts.numIters, new SampleFromSeq(List(3, 5)))
    Seq(laplace, gauss, rate, iters)
  }
  //Define model type, Binary, Multiclass, or Regression
  val searcher = new BinaryHyperparameterSearcher(opts, parameters, numTrials)
          
  // Call hyperparameter search to run and write to given file location
  val (results, report) = searcher.search(instances, instance_field)
  
  //Write to the file of your choice. 
  results.write(SequenceFile(out_dir + "/trialSummary"))
  report.write(SequenceFile(out_dir + "/parameterReport"))
}

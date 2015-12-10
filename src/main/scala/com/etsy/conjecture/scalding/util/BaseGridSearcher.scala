package com.etsy.conjecture.scalding.util


import scala.util.Random
import com.etsy.conjecture.scalding.util._
import com.etsy.conjecture.data._
import com.etsy.conjecture.model._
import com.twitter.scalding._
import cascading.tuple.Fields

/**
  * Interface for using conjecture's hyperparamter tuner
  * See DefaultGridSearcher job for an example of how to extend this class.
  */
abstract class BaseGridSearcher(args : Args) extends Job(args) {
  val input = args.getOrElse("input", "specify_an_input_dir")
  val out_dir = args.getOrElse("out_dir", "hypertuned")
  val folds = args.getOrElse("folds", "0").toInt
  val problemName = args.getOrElse("name", "demo_problem")
  val xmx = args.getOrElse("xmx", "3").toInt
  val containerMemory = (xmx * 1024 * 1.16).toInt

  // Let the user configure the field names on the command line.
  val data_field_names = args.getOrElse("data_fields", "instance").split(",")
  val data_fields = data_field_names.tail.foldLeft(new Fields(data_field_names.head)) { (x,y) => x.append(new Fields(y)) }
  val instance_field = Symbol(args.getOrElse("instance_field", "instance"))

  val salt = args.getOrElse("salt", "")
  val numTrials = args.getOrElse("num_trials", "10").toInt
  val instances = SequenceFile(input, data_fields).project(instance_field)

  //Define your parameters to optimize
  val opts: DynamicOptions
  val parameters: Seq[HyperParameter[_]]
  //Define the searcher to use based on the classifier
  val searcher: HyperparameterSearcher[_,_,_]
  override def config: Map[AnyRef, AnyRef] = super.config ++
    Map("mapred.child.java.opts" -> "-Xmx%dG".format(xmx),
        "mapreduce.map.memory.mb" -> containerMemory.toString,
        "mapreduce.reduce.memory.mb" -> containerMemory.toString
       )
  
}


package com.etsy.scalding.jobs.conjecture

import com.twitter.scalding.{Args, Job, Mode, SequenceFile, Tsv}
import com.etsy.conjecture.scalding.evaluate.BinaryCrossValidator
import com.etsy.conjecture.scalding.train.BinaryModelTrainer
import com.etsy.conjecture.data.{BinaryLabel,BinaryLabeledInstance,StringKeyedVector}
import com.etsy.conjecture.model.UpdateableLinearModel

import com.google.gson.Gson

import cascading.tuple.Fields

class AdHocClassifier(args : Args) extends Job(args) {

  val input = args.getOrElse("input", "specify_an_input_dir")
  val out_dir = args.getOrElse("out_dir", "adhoc_classifier")
  val folds = args.getOrElse("folds", "0").toInt
  val problemName = args.getOrElse("name", "demo_problem")
  val xmx = args.getOrElse("xmx", "3").toInt
  val containerMemory = (xmx * 1024 * 1.16).toInt

  // Let the user configure the field names on the command line.
  val data_field_names = args.getOrElse("data_fields", "instance").split(",")
  val data_fields = data_field_names.tail.foldLeft(new Fields(data_field_names.head)) { (x,y) => x.append(new Fields(y)) }
  val instance_field = Symbol(args.getOrElse("instance_field", "instance"))

  // assumes input instances are a sequence file
  val instances = SequenceFile(input, data_fields).project(instance_field)

  val model_pipe = new BinaryModelTrainer(args)
    .train(instances, instance_field, 'model)

  model_pipe
    .write(SequenceFile(out_dir + "/model"))
    .mapTo('model -> 'json) { x : UpdateableLinearModel[BinaryLabel] => new Gson().toJson(x) }
    .write(Tsv(out_dir + "/model_json"))

  if(folds > 0) {
    val eval_pred = new BinaryCrossValidator(args, folds)
      .crossValidateWithPredictions(instances, instance_field, 'pred)
    eval_pred._1
      .write(Tsv(out_dir + "/xval"))
    eval_pred._2
      .write(SequenceFile(out_dir + "/pred"))
  }

  override def config = super.config ++
    Map("mapred.child.java.opts" -> "-Xmx%dG".format(xmx),
        "mapreduce.map.memory.mb" -> containerMemory.toString,
        "mapreduce.reduce.memory.mb" -> containerMemory.toString
    )
}

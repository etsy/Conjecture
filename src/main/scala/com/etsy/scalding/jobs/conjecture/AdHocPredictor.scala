package com.etsy.scalding.jobs.conjecture

import com.twitter.scalding.{Args, Job, Mode, SequenceFile, Tsv}
import com.etsy.conjecture.scalding.evaluate.BinaryEvaluator
import com.etsy.conjecture.data.{BinaryLabeledInstance, BinaryLabel}
import com.etsy.conjecture.model.UpdateableLinearModel

import com.google.gson.Gson

import cascading.tuple.Fields

class AdHocPredictor(args : Args) extends Job(args) {

  val input = args.getOrElse("input", "specify_an_input_dir")
  val out_dir = args.getOrElse("out_dir", "adhoc_classifier")
  val model = args.getOrElse("model", "specify a model")
  val problemName = args.getOrElse("name", "demo_problem")
  val xmx = args.getOrElse("xmx", "3").toInt
  val skipFinalSort = args.boolean("skip_final_sort")
  val containerMemory = (xmx * 1024 * 1.16).toInt

  // Let the user configure the field names on the command line.
  val data_field_names = args.getOrElse("data_fields", "instance").split(",")
  val data_fields = data_field_names.tail.foldLeft(new Fields(data_field_names.head)) { (x,y) => x.append(new Fields(y)) }
  val model_field = new Fields(args.getOrElse("model_field", "model"))
  val instance_field = new Fields(args.getOrElse("instance_field", "instance"))

  val instances = SequenceFile(input, data_fields).read.project(instance_field)

  val model_pipe = SequenceFile(model, model_field).read

  val predictions = instances.crossWithTiny(model_pipe)
    .map((model_field, instance_field) -> ('pred, 'explain)) {
        x : (UpdateableLinearModel[BinaryLabel], BinaryLabeledInstance) =>
        (x._1.predict(x._2.getVector), x._1.explainPrediction(x._2.getVector))
    }
    .discard(model_field)
    .map(instance_field -> 'supporting_data) { x : BinaryLabeledInstance => x.getSupportingData() }
    .project('supporting_data, 'pred)
    .map('pred -> 'pred) { in : BinaryLabel => in.getValue() }

  val output = if (skipFinalSort)
    predictions
  else
    predictions.groupAll { _.sortBy('pred).reverse }

  output.write(SequenceFile(out_dir + "/pred"))

  override def config = super.config ++
    Map("mapred.child.java.opts" -> "-Xmx%dG".format(xmx),
        "mapreduce.map.memory.mb" -> containerMemory.toString,
        "mapreduce.reduce.memory.mb" -> containerMemory.toString
    )

}

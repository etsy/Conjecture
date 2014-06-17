package com.etsy.conjecture.demo

import com.twitter.scalding._
import com.etsy.conjecture.scalding.evaluate.{ MulticlassCrossValidator, MulticlassEvaluator }
import com.etsy.conjecture.scalding.train.MulticlassModelTrainer
import com.etsy.conjecture.data.{ MulticlassLabel, MulticlassLabeledInstance }
import com.etsy.conjecture.model.MulticlassLogisticRegression

import com.google.gson.Gson

import cascading.tuple.Fields

class LearnMulticlassClassifier(args: Args) extends Job(args) {

    val input = args("input")
    val out_dir = args.getOrElse("output", "multiclass_classifier")
    val class_names = args("class_names").split(",")
    val folds = args.getOrElse("folds", "0").toInt

    // Let the user configure the field names on the command line.
    val data_field_names = args.getOrElse("data_fields", "instance").split(",")
    val data_fields = data_field_names.tail.foldLeft(new Fields(data_field_names.head)) { (x, y) => x.append(new Fields(y)) }
    val instance_field = Symbol(args.getOrElse("instance_field", "instance"))

    val instances = SequenceFile(input, data_fields).project(instance_field)

    val model_pipe = new MulticlassModelTrainer(args, class_names)
        .train(instances, instance_field, 'model)

    model_pipe
        .write(SequenceFile(out_dir + "/model"))
        .mapTo('model -> 'json) { x: MulticlassLogisticRegression => new Gson().toJson(x) }
        .write(Tsv(out_dir + "/model_json"))

    if (folds > 0) {
        val eval_pred = new MulticlassCrossValidator(args, folds, class_names)
            .crossValidateWithPredictions(instances, instance_field, 'pred)
        eval_pred._1
            .write(Tsv(out_dir + "/xval"))
        eval_pred._2
            .write(Tsv(out_dir + "/pred"))
    }
}

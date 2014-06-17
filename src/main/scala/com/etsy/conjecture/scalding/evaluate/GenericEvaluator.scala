package com.etsy.conjecture.scalding.evaluate

import com.twitter.scalding._
import com.etsy.conjecture._
import com.etsy.conjecture.data._
import com.etsy.conjecture.evaluation._
import com.etsy.conjecture.model._

import cascading.pipe.Pipe

abstract class GenericEvaluator[L <: Label] extends Serializable {

    import Dsl._

    def build(): ModelEvaluation[L]

    def evaluate(instance_pipe: Pipe, predict_field: Symbol, label_field: Symbol, evaluation_field: Symbol): Pipe = {
        val partialEval = '__partial_eval
        instance_pipe
          .map((label_field, predict_field) -> partialEval){
            pair : (L, L) =>
            val eval = build
            eval.add(pair._1, pair._2)
            eval
          }
          .groupAll{
            _.reduce(partialEval -> evaluation_field){
              (eval : ModelEvaluation[L], final_eval : ModelEvaluation[L]) =>
              final_eval.merge(eval)
              final_eval
            }
          }
          .project(evaluation_field)
    }

    def evaluate(instance_pipe: Pipe, instance_field: Symbol, label_field: Symbol, model_pipe: Pipe, model_field: Symbol, evaluation_field: Symbol): Pipe = {
        val instances_with_predictions = assign_predictions(instance_pipe, instance_field, label_field, model_pipe, model_field, 'prediction)
        evaluate(instances_with_predictions, label_field, 'prediction, evaluation_field)
    }

    def assign_predictions(instance_pipe: Pipe, instance_field: Symbol, label_field: Symbol, model_pipe: Pipe, model_field: Symbol, prediction_field: Symbol = 'prediction) = {
        instance_pipe.crossWithTiny(model_pipe)
            .map((instance_field, model_field) -> (label_field, prediction_field)) { x: (LabeledInstance[L], Model[L]) => (x._1.getLabel, x._2.predict(x._1.getVector)) }
            .project(label_field, prediction_field)
    }
}

class BinaryEvaluator extends GenericEvaluator[BinaryLabel] {
    def build() = new BinaryModelEvaluation()
}

class MulticlassEvaluator(categories: Array[String]) extends GenericEvaluator[MulticlassLabel] {
    def build() = new MulticlassModelEvaluation(categories)
}

class RegressionEvaluator extends GenericEvaluator[RealValuedLabel] {
    def build() = new RegressionModelEvaluation()
}

package com.etsy.conjecture.scalding.evaluate

import com.twitter.scalding._

import cascading.pipe.Pipe
import cascading.tuple.{ Fields, TupleEntry, Tuple }

import com.etsy.conjecture.data._
import com.etsy.conjecture.evaluation._
import com.etsy.conjecture.model._
import com.etsy.conjecture.scalding.train._

class GenericCrossValidator[L <: Label, M <: UpdateableModel[L, M], E <: ModelEvaluation[L]](val evaluator: GenericEvaluator[L], val builder: AbstractModelTrainer[L, M], val folds: Int, val salt: String = "") extends Serializable {

    import Dsl._

    def crossValidateWithPredictions(pipe: Pipe, instanceField: Symbol, predictionField: Symbol, labelField: Symbol = '__actual): (Pipe, Pipe) = {
        val folded = pipe.map(instanceField -> '__fold) { li: LabeledInstance[L] => (li.getVector.hashCode.toString + salt).hashCode % folds }
            .forceToDisk
        val preds = (0 until folds).map { i: Int => predictFold(folded, '__model, instanceField, labelField, predictionField, i) }
        val eval = preds.map { i: Pipe => evaluator.evaluate(i, predictionField, labelField, '__eval) }.reduce { _ ++ _ }
            .groupAll {
                _.foldLeft('__eval -> '__eval)(new EvaluationAggregator[L]()) {
                    (a: EvaluationAggregator[L], e: E) => a.add(e); a
                }
            }
            .map('__eval -> '__eval) { a: EvaluationAggregator[L] => a.toString }
        val preds_all = preds.reduce { _ ++ _ }.project(labelField, predictionField)
        (eval, preds_all)
    }

    // note that the models on each fold may be calibrated differently, which will mess up the AUC calculation.
    // this may not be a big problem though.
    def predictFold(folded: Pipe, modelField: Symbol, instanceField: Symbol, labelField: Symbol, predictionField: Symbol, fold: Int): Pipe = {
        val train_inst = folded.filter('__fold) { x: Int => x != fold }
        val test_inst = folded.filter('__fold) { x: Int => x == fold }
        val model = builder.train(train_inst, instanceField, modelField)
        evaluator.assign_predictions(test_inst, instanceField, labelField, model, modelField, predictionField)
    }

    def crossValidate(pipe: Pipe, instanceField: Symbol): Pipe = {
        val folded = pipe.map(Fields.ALL -> '__fold) { te: TupleEntry => (te.getTuple.hashCode.toString + salt).hashCode % folds }
        // process each fold in parallel.
        val preds = (0 until folds).map { i: Int => predictFold(folded, '__model, instanceField, '__actual, '__pred, i) }
        // pull into one pipe.
        evaluator.evaluate(preds.reduce { _ ++ _ }, '__pred, '__actual, '__eval)
            .map('__eval -> '__eval) { a: EvaluationAggregator[L] => a.toString }
    }

    def evaluateFold(folded: Pipe, modelField: Symbol, instanceField: Symbol, labelField: Symbol, evalField: Symbol, fold: Int): Pipe = {
        val train_inst = folded.filter('__fold) { x: Int => x != fold }
        val test_inst = folded.filter('__fold) { x: Int => x == fold }
        val model = builder.train(train_inst, instanceField, modelField)
        evaluator.evaluate(test_inst, instanceField, labelField, model, modelField, evalField)
    }
}

class BinaryCrossValidator(args: Args, folds: Int) extends GenericCrossValidator[BinaryLabel, UpdateableLinearModel[BinaryLabel], BinaryModelEvaluation](
    new BinaryEvaluator(), new BinaryModelTrainer(args), folds, args.getOrElse("salt", ""))

class RegressionCrossValidator(args: Args, folds: Int) extends GenericCrossValidator[RealValuedLabel, UpdateableLinearModel[RealValuedLabel], RegressionModelEvaluation](
    new RegressionEvaluator(), new RegressionModelTrainer(args), folds, args.getOrElse("salt", ""))

class MulticlassCrossValidator(args: Args, folds: Int, categories: Array[String]) extends GenericCrossValidator[MulticlassLabel, MulticlassLogisticRegression, MulticlassModelEvaluation](
    new MulticlassEvaluator(categories), new MulticlassModelTrainer(args, categories), folds, args.getOrElse("salt", ""))

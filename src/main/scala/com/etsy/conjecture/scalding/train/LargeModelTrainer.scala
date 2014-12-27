package com.etsy.conjecture.scalding.train

import cascading.flow._
import cascading.operation._
import cascading.pipe._
import cascading.pipe.joiner.InnerJoin

import com.etsy.conjecture.data._
import com.etsy.conjecture.model._

import com.twitter.scalding._

class LargeModelTrainer[L <: Label, M <: UpdateableModel[L, M]](strategy: ModelTrainerStrategy[L, M], training_bins: Int) extends AbstractModelTrainer[L, M] {
    import Dsl._

    def train(instances: Pipe, instanceField: Symbol = 'instance, modelField: Symbol = 'model): Pipe = {
        trainRecursively(None, modelField, binTrainingData(instances, instanceField), instanceField, strategy.getIters)
    }

    def reTrain(instances: Pipe, instanceField: Symbol, model: Pipe, modelField: Symbol): Pipe = {
        throw new UnsupportedOperationException("not implemented due to expensiveness of model duplication")
    }

    def binTrainingData(instances: Pipe, instanceField: Symbol): Pipe = {
        instances
            .project(instanceField)
            .map(instanceField -> 'bin) { b: LabeledInstance[L] => b.hashCode % training_bins }
    }

    // This implements a full iteration of training, ending with a pipe with a model.
    protected def trainIteration(modelPipe: Option[Pipe], modelField: Symbol, instancePipe: Pipe, instanceField: Symbol): Pipe = {
        val iterationField = '__iteration__
        val modelCountField = '__model_count__
        // Subsample instances.
        val subsampled = instancePipe.filter(instanceField) { i: LabeledInstance[L] => math.random < strategy.sampleProb(i.getLabel) }
        // Get models on each mapper.
        (modelPipe match {
            case Some(pipe) => subsampled.joinWithSmaller('bin -> 'bin, pipe, new InnerJoin(), training_bins)
            case _ => subsampled.map(instanceField -> (instanceField, modelField)) { x: LabeledInstance[L] => (x, strategy.getModel) }
        })
            // Count iteration numbers.
            .insert(iterationField, 0)
            .insert(modelCountField, 1)
            // Convert instances to instance list.
            .map(instanceField -> instanceField) { i: LabeledInstance[L] => List(i) }
            // Perform map-side aggregation of models, which are then sent to a single reduce node for merging.
            .groupBy('bin) {
                _.reduce[(M, List[LabeledInstance[L]], Int, Int)](
                    (modelField, instanceField, iterationField, modelCountField) -> (modelField, instanceField, iterationField, modelCountField))(strategy.modelReduceFunction)
                    .reducers(training_bins)
            }
            .mapTo((modelField, iterationField) -> modelField) { x: (M, Int) => strategy.endIteration(x._1, x._2, training_bins) }
            // flatten submodels and aggregate on different reducers.
            .flatMapTo(modelField -> ('param, 'value)) { m: M =>
                println("epoch: " + m.getEpoch)
                m.setParameter("__epoch__", m.getEpoch)
                new Iterable[(String, Double)]() {
                    def iterator() = {
                        new Iterator[(String, Double)]() {
                            val it = m.decompose
                            def hasNext: Boolean = { it.hasNext }
                            def next: (String, Double) = { val e = it.next(); (e.getKey, e.getValue) }
                        }
                    }
                }
            }
            .groupBy('param) { _.sum[Double]('value).forceToReducers }
            // Duplicate the summed parameters rather than duplicating the reconstructed model, for speed reasons.
            .flatMapTo(('param, 'value) -> ('bin, 'param, 'value)) {
                b: (String, Double) =>
                    (0 until training_bins).map { i => (i, b._1, b._2) }
            }
            // Reconstruct the model for each bin.  Uses a hacked on Scalding operator due to kryo serialization not supporting copy().
            .groupBy('bin) {
                _.every {
                    pipe =>
                        new Every(
                            pipe,
                            ('param, 'value),
                            new FoldAggregator[(String, Double), M](
                                {
                                    (model: M, param: (String, Double)) =>
                                        if (param._1 == "__epoch__") {
                                            val epoch = (param._2 / training_bins).toLong
                                            println("epoch: " + epoch)
                                            model.setEpoch(epoch)
                                        } else {
                                            model.setParameter(param._1, param._2)
                                        }
                                        model
                                },
                                strategy.getModel,
                                modelField,
                                implicitly[TupleConverter[(String, Double)]],
                                implicitly[TupleSetter[M]]) {
                                override def start(flowProcess: FlowProcess[_], call: AggregatorCall[M]) = call.setContext(strategy.getModel)
                            })
                }
                    .reducers(training_bins)
            }
            .project('bin, modelField)
    }

    protected def trainRecursively(modelPipe: Option[Pipe], modelField: Symbol, instancePipe: Pipe, instanceField: Symbol, iterations: Int): Pipe = {
        val updatedPipe = trainIteration(modelPipe, modelField, instancePipe, instanceField)
        if (iterations == 1) {
            updatedPipe.filter('bin) { b: Int => b == 0 }.mapTo(modelField -> modelField) { strategy.modelPostProcess }.groupAll { _.pass }
        } else {
            trainRecursively(Some(updatedPipe), modelField, instancePipe, instanceField, iterations - 1)
        }
    }

}

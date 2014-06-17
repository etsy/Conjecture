package com.etsy.conjecture.demo

import com.twitter.scalding._
import com.etsy.conjecture.data._

class IrisDataToMulticlassLabeledInstances(args: Args) extends Job(args) {

    // This class just converts the tsv of iris data to a sequence file of multiclass labeled instances
    // which the AdHocClassifier can then use to train.
    // Note that for a dataset of this size, the use of a hadoop job is overkill, this is for demonstration
    // puroses.
    TextLine(args.getOrElse("input_file", "iris.tsv"))
        .mapTo('instance) {
            l: String =>
                val names = Array("sepal_length", "sepal_width", "petal_length", "petal_width")
                val parts = l.split("\t")
                val instance = new MulticlassLabeledInstance(parts(4))
                (0 until 4).foreach { i => instance.setCoordinate(names(i), parts(i).toDouble) }
                instance
        }
        .write(SequenceFile(args.getOrElse("output_file", "iris_instances")))
}

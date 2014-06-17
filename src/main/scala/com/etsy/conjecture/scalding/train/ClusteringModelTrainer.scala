package com.etsy.conjecture.scalding.train

import cascading.pipe.Pipe
import com.twitter.scalding._
import com.etsy.conjecture.data._
import com.etsy.conjecture.model._
import scala.collection.JavaConversions._
import java.io.File
import scala.io.Source

class ClusteringModelTrainer(args: Args, centers: Map[String, StringKeyedVector]) extends AbstractModelTrainer[ClusterLabel, ClusteringModel[ClusterLabel]] with ModelTrainerStrategy[ClusterLabel, ClusteringModel[ClusterLabel]] {

    // number of iterations for
    // sequential gradient descent
    var iters = args.getOrElse("iters", "1").toInt

    /*
     * Number of clusters to build
     */
    val num_clusters = args.getOrElse("num_clusters","100").toInt

    /*
     * Error tolerance for the l1 projection in 'web scale kmeans'
     */
    val error_tolerance = args.getOrElse("error_tolerance","0.01").toDouble

    /*
     * Ball radius for the l1 projection in 'web scale kmeans'
     */
    val ball_radius = args.getOrElse("ball_radius","1.0").toDouble

    override def getIters: Int = iters

    def getModel: ClusteringModel[ClusterLabel] = {
        new KMeans(centers)
        .setNumClusters(num_clusters)
        .setL1ProjectionErrorTolerance(error_tolerance)
        .setL1ProjectionBallRadius(ball_radius)
    }

    val bins = args.getOrElse("bins", "100").toInt

    val trainer = if (args.boolean("large")) new LargeModelTrainer(this, bins) else new SmallModelTrainer(this)

    def train(instances: Pipe, instanceField: Symbol = 'instance, modelField: Symbol = 'model): Pipe = {
        trainer.train(instances, instanceField, modelField)
    }

    def reTrain(instances: Pipe, instanceField: Symbol, model: Pipe, modelField: Symbol): Pipe = {
        trainer.reTrain(instances, instanceField, model, modelField)
    }

}

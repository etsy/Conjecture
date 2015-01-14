package com.etsy.scalding.jobs.conjecture

import com.twitter.scalding.{Args, Job, Mode, SequenceFile, Tsv}
import com.etsy.conjecture.data.StringKeyedVector
import cascading.pipe.Pipe
import com.twitter.scalding._
import com.etsy.conjecture.data._
import com.etsy.conjecture.model._
import scala.collection.JavaConversions._
import java.io.File
import scala.io.Source

/**
 *  Implements kmeans|| as described here: http://theory.stanford.edu/~sergei/papers/vldb12-kmpar.pdf
 *  Also includes fast L1 projection step to find sparse cluster centers as described here:
 *  http://www.eecs.tufts.edu/~dsculley/papers/fastkmeans.pdf
 *
 *  Usage:
 *    --curr_iter : Set the current iteration.
 *    --num_starting_centers : Number of starting points to select at random to initialize C.
 *    --init_iters : The number of initial iterations to do to find C oversampled centers.
 *    --finish_iters : The number of iterations to cluster the C oversampled centers into
 *                     K starting centers.
 *    --oversampling_factor : The number of points to oversample on each iteration of the
 *                            parallel kmeans initialization, described as a fraction
 *                            of the number of centers.
 *    --kmeans_iters : The number of iterations to cluster the original dataset.
 *    --input : Path on hdfs to the dataset to be clustered. Dataset should be a pipe
 *              of (id_field : String, instance_field : StringKeyedVector).
 *    --out_dir : Path where intermediate data, final cluster centers, and assignments
 *                will be written.
 *    --id_field : Symbol for the id of the point being clustered, (e.g. doc_id).
 *    --instance_field : Symbol for the point being clustered, (e.g. document).
 *    --sparsify : Whether or not to enforce cluster center sparsity.
 *    --ball_radius : Radius of ball to project cluster centers on to in l1 projection.
 *                    E.g. 10^-1 == more sparse, 10^2 == less sparse.
 *    --error_tolerance : Error tolerance in the e-accurate l1 projection.
 */
class AdHocClustererTest(args: Args) extends Job(args) {

    val curr_iter = args.getOrElse("curr_iter","0").toInt
    val num_starting_centers = args.getOrElse("num_starting_centers","10").toInt
    val init_iters = args.getOrElse("init_iters","5").toInt
    val finish_iters = args.getOrElse("finish_iters","5").toInt
    val oversampling_factor = args.getOrElse("oversampling_factor","1.0").toDouble
    val kmeans_iters = args.getOrElse("kmeans_iters","5").toInt

    val input = args.getOrElse("input", "specify_an_input_dir")
    val out_dir = args.getOrElse("out_dir", "specify_an_output_dir")+"/"
    val id_field = Symbol(args.getOrElse("id_field", "id"))
    val instance_field = Symbol(args.getOrElse("instance_field", "instance"))
    val xmx = args.getOrElse("xmx", "3").toInt
    val containerMemory = (xmx * 1024 * 1.16).toInt
    
    val max_finish_iters = init_iters + finish_iters
    val total_iter = init_iters + finish_iters + kmeans_iters

    /*
     * Number of clusters to build
     */
    val num_clusters = args.getOrElse("num_clusters","100").toInt

    /*
     * Number of centers to oversample in the initialization phase
     */
    val take_per_round = math.floor(num_clusters * oversampling_factor).toInt

    /*
     * Whether or not to enforce cluster center sparsity via l1 projection
     */
    val sparsify = args.getOrElse("sparsify","true").toBoolean

    /*
     * Error tolerance for the l1 projection
     */
    val error_tolerance = args.getOrElse("error_tolerance","0.01").toDouble

    /*
     * Ball radius for the l1 projection
     */
    val ball_radius = args.getOrElse("ball_radius","10.0").toDouble

    /**
     * Read in the pipe of data to be clustered
     */
    lazy val instances = SequenceFile(input, (id_field, instance_field)).read

    /**
     * Define centers based on the current iteration
     */
    var centers : Pipe = if(curr_iter == 0){
      /**
       * First iteration: Select some starting centers at random from the dataset
       */
      instances
        .map(instance_field -> 'rand){ i : StringKeyedVector => math.random}
        .groupAll{_.sortWithTake[(Double, StringKeyedVector)](('rand, instance_field) -> 'list, num_starting_centers){(a, b) => a._1 > b._1}}
        .map('list -> 'centers){ l : List[(Double,StringKeyedVector)] => l.map(i => i._2) }
        .project('centers)
    } else {
      /**
       *  If curr_iter <= init_iters, do kmeans|| iterations.
       *
       *  If init_iters < curr_iter <= max_finish_iters, cluster the oversampled set of initial centers 
       *  into num_clusters true initial centers.
       *
       *  If max_finish_iter < curr_iter <= total_iter, cluster the initial dataset using 
       *  the clusters obtained from previous steps as initial centers.
       */
      SequenceFile(out_dir+"iter_"+(curr_iter - 1)+"/centers", ('centers)).read
    }

    lazy val oversampled_cluster_centers = SequenceFile(out_dir+"iter_"+(init_iters - 1)+"/centers", ('centers)).read
      .flattenTo[StringKeyedVector]('centers -> 'center)
      .rename('center -> instance_field)

    val new_centers = if (curr_iter < init_iters) {
      /** Over sample (oversampling_factor * num_clusters) factors **/
      kmeansPlusPlusIter(instances, centers, take_per_round)
    } else if (curr_iter == init_iters) {
      /** Get ready to cluster oversampled factors by doing a kmeans++ pass over them **/
      val init_final_centers = kmeansPlusPlusReclusterInit(centers, num_clusters)
      kmeansIter(oversampled_cluster_centers, init_final_centers, num_clusters, instance_field, curr_iter)
    } else if (curr_iter <= max_finish_iters) {
      /** Recluster oversampled centers into num_clusters final centers **/
      kmeansIter(oversampled_cluster_centers, centers, num_clusters, instance_field, curr_iter)
    } else {
      /** Cluster the original dataset **/
      kmeansIter(instances, centers, num_clusters, instance_field, curr_iter)
    }

    /** 
     *  If it's the last iteration, flatten the centers and 
     *  write them out; optionally generate cluster assignments
     *  for each instance. Else, write the centers map out at 
     *  the end of each iteration.
     */
    if(curr_iter == total_iter){
      new_centers
      .flattenTo[StringKeyedVector]('centers -> 'center)
      .project('center)
      .write(SequenceFile(out_dir+"centers"))

      if(args.boolean("generate_assignments")) {
        instances
        .crossWithTiny(new_centers)
        .map((instance_field, 'centers) -> 'cluster_assignment){ i : (StringKeyedVector, Map[String, StringKeyedVector]) => assignCluster(i._1, i._2) }
        .project(id_field, 'cluster_assignment)
        .write(SequenceFile(out_dir+"assignments"))
      }
    } else {
      new_centers.write(SequenceFile(out_dir+"iter_"+curr_iter+"/centers"))
    }

    /**
     *  Kmeans|| initialization:
     *    C <- sample some points uniformly at random from instances
     *    For init_iters:
     *      C' <- top take_per_round points in instances by distance to current centers C
     *      C  <- union(C,C')
     */
    def kmeansPlusPlusIter(instances : Pipe, centers : Pipe, take_per_round : Int) : Pipe = {
      /** Get each points' distance to it's nearest cluster center **/
      val closest_distances = instances
        .crossWithTiny(centers)
        .map((instance_field, 'centers) -> 'closest_distance){ in : (StringKeyedVector, List[StringKeyedVector]) => distanceToClosestCenter(in._1, in._2) }

      /** Sum all closest distances into a normalizer **/
      val normalizer = closest_distances
        .groupAll{ _.sum[Double]('closest_distance -> 'denominator) }
    
      /** 
       * Normalize each points' distance to it's nearest cluster center to a probability. 
       * Take the top take_per_round descending points as new centers.
       */
      val top_by_distance = closest_distances
        .crossWithTiny(normalizer)
        .map(('closest_distance, 'denominator) -> 'normalized_distance){ i : (Double, Double) => i._1 / i._2 }
        .groupAll{ _.sortWithTake[(Double, StringKeyedVector)](('normalized_distance, instance_field) -> 'top_by_distance, take_per_round){(a, b) => a._1 > b._1} }
        .flattenTo[(Double, StringKeyedVector)]('top_by_distance -> ('distance, instance_field))
        .project(instance_field)
    
      /** Union the set of new centers and old centers **/
      val new_centers = ((top_by_distance.rename(instance_field -> 'center)) ++ (centers.flattenTo[StringKeyedVector]('centers -> 'center)))
        .groupAll{ _.toList[StringKeyedVector]('center -> 'centers) }
      new_centers
    }

    /**
     *  Recluster the points in C into the final num_clusters kmeans|| centers.
     */
    def kmeansPlusPlusReclusterInit(centers : Pipe, num_clusters : Int) : Pipe = {  
      centers
        .map('centers -> 'centers){ data : List[StringKeyedVector] =>
          val rand_idx = scala.util.Random.nextInt(data.size)
          val starting_C = data(rand_idx)
          var starting_centers = List(starting_C)
          val init_centers = kmeansPlusPlusInit(num_clusters, data, starting_centers)     
          init_centers.zipWithIndex.map(i=> (i._2.toString,i._1)).toMap
        }
    }

    /**
     *  Takes a pipe of points to cluster, a pipe of grouped clusters
     */
    def kmeansIter(data : Pipe, centers : Pipe, K : Int, point_sym : Symbol, iter : Int) : Pipe = {
      val data_with_centers = data.crossWithTiny(centers)
  
      val cluster_assignments = data_with_centers
        .map((point_sym, 'centers) -> 'assignment){
          fields : (StringKeyedVector, Map[String,StringKeyedVector]) =>
          val (point, centers) = fields
          assignCluster(point, centers) 
        }
        .project(point_sym, 'assignment)

      val grouped = cluster_assignments
        .groupBy('assignment){ _.size('denom).reduce[StringKeyedVector](point_sym){(a, b) => a.add(b); a} }
        .map((point_sym, 'denom) -> 'cluster){
          fields : (StringKeyedVector, Double) =>
          var (centroid, denom) = fields
          centroid.mul(1.0/denom)
          if(sparsify){
            l1Projection(centroid, error_tolerance, ball_radius)
          }
          centroid
        }
        .project('assignment, 'cluster)

      val debug = grouped
        .map('cluster -> 'top){ i : StringKeyedVector => i.getMap().toList.sortBy(_._2).reverse.take(100).map(i => i._1).mkString(" ") }
        .project('assignment, 'top)
        .write(SequenceFile(out_dir+"debug/iter_"+iter+"_top_terms"))
  
      grouped
      .groupAll{ _.toList[(String,StringKeyedVector)](('assignment, 'cluster) -> 'centers) }
      .map('centers -> 'centers){ l : List[(String, StringKeyedVector)] => l.toMap }
    }

    /**
     *  Generates initial centers for kmeans clustering to speed up convergence.
     *  See more here: http://ilpubs.stanford.edu:8090/778/1/2006-13.pdf
     */
    def kmeansPlusPlusInit(iters : Int, data : List[StringKeyedVector], centers : List[StringKeyedVector]) : List[StringKeyedVector] = {
      var new_centers = centers
      var temp_data = data
      (0 until iters).foreach{ iter =>
        val dists = temp_data.map(i => (i, distanceToClosestCenter(i, centers)))
        val norm = dists.map(i => i._2).sum
        val x = dists.map(i => (i._1, i._2/norm)).sortBy(_._2).reverse.map(i=>i._1).take(1)(0)
        new_centers = (new_centers ++ List(x)).toSet.toList
        temp_data = temp_data.filter(i => i != x)
      }
      new_centers
    }

    /**
     *  Returns the cosine distance between a point and its closest center.
     */
    def distanceToClosestCenter(point : StringKeyedVector, centers : List[StringKeyedVector]) : Double = {
      centers.map(center => computeDistance(point, center)).min
    }

    /**
     *  Computes the cosine distance between a point and a center.
     */
    def computeDistance(point : StringKeyedVector, center : StringKeyedVector) : Double = {
      val dot_product = point.dot(center)
      val point_magnitude = point.LPNorm(2.0)
      val center_magnitude = center.LPNorm(2.0)
      1.0 - (dot_product/(point_magnitude*center_magnitude))
    }

    /**
     *  Assign a point to its nearest cluster center by cosine distance.
     */
    def assignCluster(point : StringKeyedVector, centers : Map[String,StringKeyedVector]) : String = {
      val distances = centers.toList.map(i => (i._1, computeDistance(point, i._2)))
      distances.minBy{_._2}._1
    }

    /**
     *  e-Accurate Projection to L1 ball for sparse cluster centers
     */
    def l1Projection(center : StringKeyedVector, e : Double = 0.01, lambda : Double = 1.0) : StringKeyedVector = {
      val l1Norm = center.LPNorm(1.0)
      if (l1Norm <= lambda + e) {
        center
      } else {
        var upper = center.max()
        var lower = 0.0
        var current = l1Norm
        var theta = 0.0
        while (current > lambda*(1+e) || current < lambda) {
          theta = (upper + lower) / 2.0
          current = center.values().map(i => math.max(0.0, math.abs(i)-theta)).sum
          if(current <= lambda){
            upper = theta
          } else {
            lower = theta
          }
        }
        var sparse_center = new StringKeyedVector()
        center.getMap()
        .map(i => (i._1, math.signum(i._2) * math.max(0.0, math.abs(i._2) - theta)))
        .filter(i => i._2 != 0.0)
        .foreach{ i => sparse_center.setCoordinate(i._1, i._2)}
        sparse_center
      }
    }

    override def next : Option[Job] = { 
      val new_args = args + ("curr_iter", Some((curr_iter+1).toString))
      if(curr_iter < total_iter) {
        Some(clone(new_args))
      } else {
        None
      }   
    }

    override def config = super.config ++
      Map("mapred.child.java.opts" -> "-Xmx%dG".format(xmx),
        "mapreduce.map.memory.mb" -> containerMemory.toString,
        "mapreduce.reduce.memory.mb" -> containerMemory.toString
      )
}

package com.etsy.conjecture.scalding

import collection.mutable.PriorityQueue

import cascading.pipe.Pipe
import cascading.pipe.joiner.InnerJoin

import org.apache.commons.math3.linear.RealVector

/**
 * Class provides functions for doing approximate K-nearest neighbors.
 * hashes : The number of times to hash.
 * planes : The number of dividing planes (also bits in the hash).
 * max_bin_size : The max size for a hash bin to be considered (we do exact knn in each bin, so large ones will increase computation time).
 * parallelism : How many reducers to use for critical sections.
 * defaults are sane for most problems.
 * more hashes = more chance for true knn to be in the same hash bin as the target, but also means more computation.
 * more planes = less items in each hash bucket, which improves computation but also could degrade approximation quality.
 */ 
class LSH(val hashes : Int = 50, val planes : Int = 12, val max_bin_size : Int = 10000, val parallelism : Int = 500) extends Serializable {
  
  // import neede to write scalding-like code.
  import com.twitter.scalding.Dsl._

  /**
   * Just a class to hold an id and a vector together.
   */
  class Point[T](val id : T, val vector : RealVector) extends Serializable {}

  /**
   * Brute force knn for inside each hash bin.
   * Works faster than just using obvious scala ways (map/sortBy etc).
   */
  def findKnn[T](vec : RealVector, points : Iterable[Point[T]], K : Int) : List[(Point[T], Double)] = { 
    val q = new PriorityQueue[(Point[T], Double)]()(Ordering.by[(Point[T], Double), Double](_._2))
    var worst = 0.0 
    var size = 0 
    points.foreach{p : Point[T] =>
      val dist = p.vector.getDistance(vec)
      if(size < K || dist < worst) {
        size += 1
        q.enqueue((p, dist))
        if(size > K) {
          q.dequeue
          size -= 1
        }   
        worst = q.head._2
      }   
    }   
    q.toList.sortBy{_._2}
  }

  /**
   * Hash repeatedly by dividing the space along origin-containing planes.
   * v : The vector to hash.
   * output is the list of hashes, each having its index as part of the value.
   */ 
  def hash(v : RealVector) : IndexedSeq[Long] = {
    (0 until hashes).map{h =>
      (0 until planes).map{i =>
        val r = new scala.util.Random(i+1000*h) // random suck with lil seeds.
        val d = v.toArray.map{_*r.nextGaussian}.sum
        if(d > 0.0)
          1L << i
        else
          0L
      }.sum + (h.toLong << planes)
    }
  }

  /**
   * Forms hash bins from a single pipe of vectors and ids.
   */
  def form_bins[I](p : Pipe, id_field : Symbol, vec_field : Symbol, bin_field : Symbol, hash_field : Symbol) : Pipe = {
    p
    .map((id_field, vec_field) -> 'point){x : (I, RealVector) => new Point[I](x._1, x._2)}
    .flatMap(vec_field -> hash_field){v : RealVector => hash(v)}
    .project('point, hash_field)
    .groupBy(hash_field){
      _.size('count)
      .sortWithTake[Point[I]]('point -> bin_field, max_bin_size){(a,b) => false}
      .reducers(parallelism)
      .forceToReducers
    }
    .filter('count){c : Int => c <= max_bin_size}
    .project(hash_field, bin_field)
  }

  /**
   * Single pipe version of knn.
   * Finds knn of each element in the pipe (i.e., every element is both a target and a candidate neighbor)
   * A thing isnt its own nearest neighbor.
   * I is the type of id used.
   */
  def knn[I](p : Pipe, id_field : Symbol, vec_field : Symbol, neighbors_field : Symbol, K : Int) : Pipe = {
    form_bins[I](p, id_field, vec_field, 'bin, 'hash)
    .flatMapTo('bin -> (id_field, neighbors_field)){
      bin : List[Point[I]] =>
      bin.view.map{p =>
        (p.id, findKnn[I](p.vector, bin, K+1).filter{_._1.id != p.id}.map{t => (t._1.id, t._2)}) // (id, distance)
      }
    }
    // - aggregate knn across hash bins.
    .groupBy(id_field) {
      _.reduce[List[(I, Double)]](neighbors_field){(a, b) => (a ++ b).groupBy{_._1}.mapValues{_.head._2}.toList.sortBy{_._2}.take(K)}
      .forceToReducers
      .reducers(parallelism)
    }
    .project(id_field, neighbors_field)
  }

  /**
   * Two pipe version of knn.
   * First pipe is targets (things we find the knn for) ids are of type I
   * Second pipe is candidates (things that can be the knn) ids are of type J
   * A thing can be its own neighbor if its in both pipes.
   */
  def knn[I,J](targets : Pipe, target_id_field : Symbol, target_vec_field : Symbol,
    candidates : Pipe, candidate_id_field : Symbol, candidate_vec_field : Symbol,
    neighbors_field : Symbol, K : Int) : Pipe = {
    form_bins[I](targets, target_id_field, target_vec_field, 'target_bin, 'hash)
    .joinWithSmaller('hash -> 'hash, form_bins[J](candidates, candidate_id_field, candidate_vec_field, 'candidate_bin, 'hash), new InnerJoin(), parallelism)
    .flatMapTo(('target_bin, 'candidate_bin) -> (target_id_field, neighbors_field)){
      x : (List[Point[I]], List[Point[J]]) =>
      x._1.view.map{p =>
        (p.id, findKnn[J](p.vector, x._2, K).map{t => (t._1.id, t._2)}) // (id, distance)
      }
    }
    // - aggregate knn across hash bins.
    .groupBy(target_id_field) {
      _.reduce[List[(J, Double)]](neighbors_field){(a, b) => (a ++ b).groupBy{_._1}.mapValues{_.head._2}.toList.sortBy{_._2}.take(K)}
      .forceToReducers
      .reducers(parallelism)
    }
    .project(target_id_field, neighbors_field)
  }
}

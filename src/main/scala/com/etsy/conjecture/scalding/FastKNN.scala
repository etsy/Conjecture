package com.etsy.conjecture.scalding

import collection.mutable.PriorityQueue
import com.twitter.scalding._
import cascading.pipe.Pipe
import cascading.pipe.joiner.InnerJoin
import org.apache.commons.math3.linear.{MatrixUtils, RealVector}

object FastKNN extends Serializable {

  import com.twitter.scalding.Dsl._

  // The basic idea is that we do KNN on arbitrary types.
  // These can be objects containing e.g., identifiers (user_id etc) and also correspond to some point in a metric space.
  // Examples are objects like (user_id, vector from matrix factorization model).
  // Typically when we do the KNN procedure, we dont actually care about returning the entire object for all the neighbors,
  // but only the list of the ids, and their distances.
  // E.g., we would return the list of (user_id, distance) rather than (user_id, vector, distance).
  // This allows having larger lists of stuff in ram since the vector etc may be large.

  // The main entry point for knn in a single pipe.
  // X: Type of the element on which the distance is defined (the thing in the vec_field).
  // Y: Type of id for the element (thing in the id_field).
  // p: Pipe of stuff to knn
  // id_field: Field name for id
  // vec_field: field name for vec.
  // neighb_field: field name for result (neighbors).
  // k: the k from knn.
  // dist: the distance function for X.  If the thing you give isnt a real distance function then probably this method will give you garbage results.
  // init_num_centers: Number of blocks to partition the data into, should be probably around sqrt(n).
  // bin_per_point: how many blocks to put each point into (increasing quality of the approximation).
  def knn[X, Y](p : Pipe, id_field : Symbol, vec_field : Symbol, neighb_field : Symbol, k : Int, dist : (X, X) => Double,
    init_num_centers : Int = 10000, bins_per_point : Int = 5, max_bin_size : Int = 20000) : Pipe = {

    val centers = initialize_bins[X](p, id_field, vec_field, dist, init_num_centers, bins_per_point, max_bin_size)

    // Do knn in each cluster, and aggregate.
    construct_bins[X, Y](p, id_field, vec_field, 'list, centers, bins_per_point, max_bin_size, dist)
      .filter('count){c : Int => c <= max_bin_size}
      .flatMapTo('list -> (id_field, neighb_field)){l : List[(Y, X)] =>
        println(l.size)
        l.view.map{t => (t._1, knn_id[X, Y](t._2, l, k+1, dist).filter{_._1 != t._1})}
      }
      .groupBy(id_field){_.reduce[List[(Y, Double)]](neighb_field){(a, b) => (a++b).groupBy{_._1}.toList.map{t => (t._1, t._2.map{_._2}.min)}.sortBy{_._2}.take(k)}.reducers(1000).forceToReducers}
      .project(id_field, neighb_field)
  }

  // The entry point for the 2 pipe version of knn.
  // Z is the type for the id field of the candidates.
  def knn2[X, Y, Z](targets : Pipe, target_id_field : Symbol, target_vec_field : Symbol,
    candidates : Pipe, candidate_id_field : Symbol, candidate_vec_field : Symbol, neighb_field : Symbol, k : Int, dist : (X, X) => Double,
    init_num_centers : Int, bins_per_point : Int, max_bin_size : Int) : Pipe = {

    // Tesselate the candidates.
    val candidate_centers = initialize_bins[X](candidates, candidate_id_field, candidate_vec_field, dist, init_num_centers, 1, max_bin_size)

    val candidate_assignments = construct_bins[X, Y](candidates, candidate_id_field, candidate_vec_field, 'candidate_list,
      candidate_centers, 1, max_bin_size, dist)

    // Assign targets to same bins as candidates.
    val target_assignments = assign_bins[X](targets, target_id_field, target_vec_field, candidate_centers, bins_per_point, dist)

    // Replicate the candidates, and fragment the targets.
    val bin_replicates = target_assignments
      .groupBy('bin){_.size('count)}
      .map('count -> 'num_fragments){c : Int => 1 + (c / max_bin_size)}
      .groupAll{_.toList[(Int, Int)](('bin, 'num_fragments) -> 'bin_replicates)}
      .mapTo('bin_replicates -> 'bin_replicates){l : List[(Int, Int)] => l.toMap}

    val targets_fragmented = target_assignments
      .crossWithTiny(bin_replicates)
      .map((target_id_field, 'bin, 'bin_replicates) -> ('rep_bin, 'rep)){x : (Z, Int, Map[Int, Int]) => (x._2, math.abs(x._1.hashCode) % x._3.getOrElse(x._2, 1))}
      .groupBy('rep_bin, 'rep){_.toList[(Z, X)]((target_id_field, target_vec_field) -> 'target_list).reducers(1000)}

    val candidates_replicated = candidate_assignments
      .crossWithTiny(bin_replicates)
      .flatMap(('bin, 'bin_replicates) -> ('rep_bin, 'rep)){x : (Int, Map[Int, Int]) => (0 until x._2.getOrElse(x._1, 1)).map{i => (x._1, i)}}
      .project('rep_bin, 'rep, 'candidate_list)

    // Do knn in each cluster, and aggregate.
    candidates_replicated
      .joinWithSmaller(('rep_bin, 'rep) -> ('rep_bin, 'rep), targets_fragmented, new InnerJoin(), 1000)
      .flatMapTo(('target_list, 'candidate_list) -> (target_id_field, neighb_field)){x : (List[(Z, X)], List[(Y, X)]) =>
        println(x._1.size + " " + x._2.size)
        x._1.view.map{t => (t._1, knn_id[X, Y](t._2, x._2, k, dist))}
      }
      .groupBy(target_id_field){_.reduce[List[(Y, Double)]](neighb_field){(a, b) => (a++b).groupBy{_._1}.toList.map{t => (t._1, t._2.map{_._2}.min)}.sortBy{_._2}.take(k)}.reducers(1000)}
      .project(target_id_field, neighb_field)
  }

  // Return the ids of the closest elements to the target.
  // X is the type of the element on which the distance is defined.
  // Y is the type of the identifier for each element.
  def knn_id[X, Y](target : X, candidates : List[(Y, X)], K : Int, dist : (X, X) => Double) : List[(Y, Double)] = {
    if(K > 250) {
      candidates.map{s => (s._1, dist(target, s._2))}.sortBy{_._2}.take(K)
    } else {
      val q = new PriorityQueue[(Y, Double)]()(Ordering.by[(Y, Double), Double](_._2))
      var worst = 0.0
      var size = 0
      candidates.foreach{s =>
        val ds = dist(target, s._2)
        if(size < K || ds < worst) {
          size += 1
          q.enqueue((s._1, ds))
          if(size > K) {
            q.dequeue
            size -= 1
          }
          worst = q.head._2
        }
      }
      q.toList.sortBy{_._2}
    }
  }

  // Return the indices of the closest elements.
  def knn_idx[X](vec : X, l : List[X], K : Int, dist : (X, X) => Double) : List[Int] = {
    val q = new PriorityQueue[(Int, Double)]()(Ordering.by[(Int, Double), Double](_._2))
    var worst = 0.0
    var size = 0
    var idx = 0
    l.foreach{r : X =>
      val di = dist(vec, r)
      if(size < K || di < worst) {
        size += 1
        q.enqueue((idx, di))
        if(size > K) {
          q.dequeue
          size -= 1
        }
        worst = q.head._2
      }
      idx += 1
    }
    q.toList.sortBy{_._2}.map{_._1}
  }

  def initialize_bins[X](p : Pipe, id_field : Symbol, vec_field : Symbol, dist : (X, X) => Double,
    init_num_centers : Int, bins_per_point : Int, max_bin_size : Int) : Pipe = {

    // Choose init_num_centers points at random.
    val centers = p
      .map(vec_field -> 'rand){r : X => new scala.util.Random(r.toString.hashCode).nextDouble}
      .groupRandomly(math.min(1000, init_num_centers)){_.sortWithTake[(X, Double)]((vec_field, 'rand) -> 'centers, 1 + (init_num_centers / 1000)){(a, b) => a._2 > b._2}}
      .groupAll{_.reduce[List[(X, Double)]]('centers){(a, b) => a++b}}
      .mapTo('centers -> 'centers){l : List[(X, Double)] => l.sortBy{-_._2}.take(init_num_centers).map{_._1}}

    val centers_new = p.crossWithTiny(centers)
      .flatMap(('centers, vec_field) -> 'bin){x : (List[X], X) => knn_idx[X](x._2, x._1, bins_per_point, dist)}
      .project('bin, vec_field)
      .map(vec_field -> 'rand){r : X => new scala.util.Random((r.toString+"foo").hashCode).nextDouble}
      .groupBy('bin){_.size('count).sortWithTake[(X, Double)]((vec_field, 'rand) -> 'centers, 1000){(a, b) => a._2 > b._2}}
      .filter('count){c : Int => c > max_bin_size}
      .mapTo(('centers, 'count) -> 'centers_new){l : (List[(X, Double)], Int) => 
        val md = l._1.maxBy{_._2}._2
        l._1.sortBy{t => val d = t._2 / md; -d * (1-d)}.take(l._2 / max_bin_size).map{_._1}
      }
      .groupAll{_.reduce[List[(X, Double)]]('centers_new){(a, b) => a++b}}

    centers.crossWithTiny(centers_new)
      .mapTo(('centers, 'centers_new) -> 'centers){x : (List[X], List[X]) => x._1 ++ x._2}
  }

  def assign_bins[X](p : Pipe, id_field : Symbol, vec_field : Symbol, centers : Pipe, bins_per_point : Int, dist : (X, X) => Double) : Pipe = {
    // Make assignments to clusters.
    p.crossWithTiny(centers)
      .flatMap((vec_field, 'centers) -> 'bin){x : (X, List[X]) => knn_idx[X](x._1, x._2, bins_per_point, dist)}
      .discard('centers)
  }

  def construct_bins[X, Y](p : Pipe, id_field : Symbol, vec_field : Symbol, list_field : Symbol,
    centers : Pipe, bins_per_point : Int, max_bin_size : Int, dist : (X, X) => Double) : Pipe = {
    assign_bins[X](p, id_field, vec_field, centers, bins_per_point, dist)
      .groupBy('bin){
        //_.toList[(Y, X)]((id_field, vec_field) -> list_field)
        _.sortWithTake[(Y, X)]((id_field, vec_field) -> list_field, max_bin_size){(a, b) => false}
        .size('count)
        .reducers(1000)
      }
      .project('bin, 'count, list_field)
  }
}

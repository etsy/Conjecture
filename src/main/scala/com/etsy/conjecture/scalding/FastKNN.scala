package com.etsy.conjecture.scalding

import collection.mutable.PriorityQueue
import com.twitter.scalding._
import cascading.pipe.Pipe
import cascading.pipe.joiner.InnerJoin
import org.apache.commons.math3.linear.{MatrixUtils, RealVector}

object FastKNN extends Serializable {

  import com.twitter.scalding.Dsl._

  def knn_id[I](vec : RealVector, l : List[(I, RealVector)], K : Int) : List[(I, Double)] = {
    if(K > 250) {
      l.map{t => (t._1, t._2.getDistance(vec))}.sortBy{_._2}.take(K)
    } else {
      val q = new PriorityQueue[(I, Double)]()(Ordering.by[(I, Double), Double](_._2))
      var worst = 0.0
      var size = 0
      l.foreach{t =>
        val dist = t._2.getDistance(vec)
        if(size < K || dist < worst) {
          size += 1
          q.enqueue((t._1, dist))
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

  def knn_idx(vec : RealVector, l : List[RealVector], K : Int) : List[Int] = {
    val q = new PriorityQueue[(Int, Double)]()(Ordering.by[(Int, Double), Double](_._2))
    var worst = 0.0
    var size = 0
    var idx = 0
    l.foreach{r : RealVector =>
      val dist = r.getDistance(vec)
      if(size < K || dist < worst) {
        size += 1
        q.enqueue((idx, dist))
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

  def initialize_bins(p : Pipe, id_field : Symbol, vec_field : Symbol,
    init_num_centers : Int, bins_per_point : Int, max_bin : Int, min_bin : Int) : Pipe = {

    // Choose init_num_centers points at random.
    val init_centers = p
      .map(vec_field -> 'rand){r : RealVector => new scala.util.Random(r.hashCode).nextDouble}
      .groupRandomly(math.min(1000, init_num_centers)){_.sortWithTake[(RealVector, Double)]((vec_field, 'rand) -> 'centers, 1 + (init_num_centers / 1000)){(a, b) => a._2 > b._2}}
      .groupAll{_.reduce[List[(RealVector, Double)]]('centers){(a, b) => a++b}}
      .mapTo('centers -> 'centers){l : List[(RealVector, Double)] => l.sortBy{-_._2}.take(init_num_centers).map{_._1}}

    // Map each element to the nearest center.
    val init_assignments = p.crossWithTiny(init_centers)
      .flatMap((vec_field, 'centers) -> 'bin){x : (RealVector, List[RealVector]) => knn_idx(x._1, x._2, 1)}
      .map((vec_field, 'centers, 'bin) -> 'dist){x : (RealVector, List[RealVector], Int) => x._2(x._3).getDistance(x._1)}
      .map(vec_field -> 'rand){r : RealVector => new scala.util.Random((r.toString+"_").hashCode).nextDouble}
      .project(id_field, vec_field, 'bin, 'rand, 'dist)

    val cluster_radii = p.crossWithTiny(init_centers)
      .mapTo((vec_field, 'centers) -> 'radii){x : (RealVector, List[RealVector]) =>
        var imin = 0
        var dmin = -1.0
        var i = 0
        val a = x._2.map{v =>
          val d = v.getDistance(x._1)
          if(d < dmin || dmin < 0){
            dmin = d
            imin = i
          }
          i += 1
          d
        }.toArray
        a(imin) = -1
        a
      }
      .groupAll{_.reduce[Array[Double]]('radii){(a, b) => var i = 0; while(i < a.size){a(i) = if(a(i) == -1.0) b(i) else if (b(i) == -1.0) a(i) else math.min(a(i), b(i)); i += 1}; a}}

    // Try to rebalance the clustering a lil.
    init_assignments
      .groupBy('bin){_.size('count).sortWithTake[(RealVector, Double)]((vec_field, 'rand) -> 'new_centers, 100){(a, b) => a._2 > b._2}}
      .filter('count){c : Int => c * bins_per_point >= min_bin}
      .crossWithTiny(init_centers)
      .crossWithTiny(cluster_radii)
      .map(('bin, 'centers, 'radii) -> ('vec, 'rad)){x : (Int, List[RealVector], Array[Double]) => (x._2(x._1), x._3(x._1))}
      .flatMapTo(('count, 'vec, 'new_centers, 'rad) -> 'center_new){x : (Int, RealVector, List[(RealVector, Double)], Double) =>
        if(x._1 * bins_per_point <= max_bin){
          List(x._2)
        } else {
          //List(x._2) ++ x._3.map{_._1}.take(1 + (x._1 * bins_per_point) / max_bin)
          List(x._2) ++ (x._3.filter{_._1.getDistance(x._2) > 0.0}.take(1 + (x._1 * bins_per_point) / max_bin).map{t =>
            val d = t._1.combineToSelf(1, -1, x._2)
            val scale = x._4 / (2 * d.getNorm)
            d.combineToSelf(scale, 1, x._2)
          })
        }
      }
      .groupAll{_.toList[RealVector]('center_new -> 'centers)}
  }

  def assign_bins(p : Pipe, id_field : Symbol, vec_field : Symbol, centers : Pipe, bins_per_point : Int) : Pipe = {
    // Make assignments to clusters.
    p.crossWithTiny(centers)
      .flatMap((vec_field, 'centers) -> 'bin){x : (RealVector, List[RealVector]) => knn_idx(x._1, x._2, bins_per_point)}
      .discard('centers)
  }

  def construct_bins[I](p : Pipe, id_field : Symbol, bin_vec_field : Symbol, dist_vec_field : Symbol, list_field : Symbol, centers : Pipe, bins_per_point : Int, max_bin : Int) : Pipe = {
    assign_bins(p, id_field, bin_vec_field, centers, bins_per_point)
      .map((id_field, dist_vec_field) -> list_field){x : (I, RealVector) => List(x)}
      .insert('count, 1)
      .groupBy('bin){
        _.reduce[(Int, List[(I, RealVector)])]('count, list_field){(a, b) =>
          if(a._1 >= max_bin) (a._1 + b._1, a._2)
          else if(b._1 >= max_bin) (a._1 + b._1, b._2)
          else if(a._1 + b._1 >= max_bin) (a._1 + b._1, (a._2 ++ b._2).take(max_bin))
          else (a._1 + b._1, a._2 ++ b._2)
        }
        .reducers(1000)
      }
      .filter('count){c : Int => c <= max_bin}
      .project('bin, list_field)
  }

  def random_proj(v : RealVector, dim : Int, seed : Long = 13376668008135L) : RealVector = {
    val r = new scala.util.Random(seed)
    MatrixUtils.createRealVector((0 until dim).map{i => (0 until v.getDimension).map{j => r.nextGaussian * v.getEntry(j)}.sum}.toArray)
  }

  def knn_inner[I](p : Pipe, id_field : Symbol, bin_vec_field : Symbol, dist_vec_field : Symbol, neighb_field : Symbol, k : Int,
    init_num_centers : Int = 10000, bins_per_point : Int = 5, max_bin : Int = 5000, min_bin : Int = 50, discard_thresh : Int = 5) : Pipe = {

    val centers = initialize_bins(p, id_field, bin_vec_field, init_num_centers, bins_per_point, max_bin, min_bin)

    // Do knn in each cluster, and aggregate.
    construct_bins[I](p, id_field, bin_vec_field, dist_vec_field, 'list, centers, bins_per_point, max_bin*discard_thresh)
      .flatMapTo('list -> (id_field, neighb_field)){l : List[(I, RealVector)] =>
        println(l.size)
        l.view.map{t => (t._1, knn_id[I](t._2, l, k+1).filter{_._1 != t._1})}
      }
      .groupBy(id_field){_.reduce[List[(I, Double)]](neighb_field){(a, b) => (a++b).groupBy{_._1}.toList.map{t => (t._1, t._2.map{_._2}.min)}.sortBy{_._2}.take(k)}.reducers(1000).forceToReducers}
      .project(id_field, neighb_field)
  }

  def knn[I](p : Pipe, id_field : Symbol, vec_field : Symbol, neighb_field : Symbol, k : Int,
    init_num_centers : Int = 10000, bins_per_point : Int = 5, max_bin : Int = 5000, min_bin : Int = 50, discard_thresh : Int = 5) : Pipe = {

    knn_inner[I](p, id_field, vec_field, vec_field, neighb_field, k, init_num_centers, bins_per_point, max_bin, min_bin, discard_thresh)
  }

  def knn_rp[I](p : Pipe, id_field : Symbol, vec_field : Symbol, neighb_field : Symbol, k : Int, dim : Int = 50,
    init_num_centers : Int = 10000, bins_per_point : Int = 5, max_bin : Int = 5000, min_bin : Int = 50, discard_thresh : Int = 5) : Pipe = {

    val rp = p.map(vec_field -> 'rp_vec){r : RealVector => random_proj(r, dim)}
    knn_inner[I](rp, id_field, 'rp_vec, vec_field, neighb_field, k, init_num_centers, bins_per_point, max_bin, min_bin, discard_thresh)
  }

  def knn2_inner[I,J](targets : Pipe, target_id_field : Symbol, target_bin_vec_field : Symbol, target_dist_vec_field : Symbol,
    candidates : Pipe, candidate_id_field : Symbol, candidate_bin_vec_field : Symbol, candidate_dist_vec_field : Symbol, neighb_field : Symbol, k : Int,
    init_num_centers : Int, bins_per_point : Int, max_bin : Int, min_bin : Int, discard_thresh : Int) : Pipe = {

    // Tesselate the candidates.
    val candidate_centers = initialize_bins(candidates, candidate_id_field, candidate_bin_vec_field, init_num_centers, 1, max_bin, min_bin)

    val candidate_assignments = construct_bins[J](candidates, candidate_id_field, candidate_bin_vec_field, candidate_dist_vec_field, 'candidate_list,
      candidate_centers, 1, max_bin * discard_thresh)

    // Assign targets to same bins as candidates.
    val target_assignments = assign_bins(targets, target_id_field, target_bin_vec_field, candidate_centers, bins_per_point)

    // Replicate the candidates, and fragment the targets.
    val bin_replicates = target_assignments
      .groupBy('bin){_.size('count)}
      .map('count -> 'num_fragments){c : Int => 1 + (c / max_bin)}
      .groupAll{_.toList[(Int, Int)](('bin, 'num_fragments) -> 'bin_replicates)}
      .mapTo('bin_replicates -> 'bin_replicates){l : List[(Int, Int)] => l.toMap}

    val targets_fragmented = target_assignments
      .crossWithTiny(bin_replicates)
      .map((target_id_field, 'bin, 'bin_replicates) -> ('rep_bin, 'rep)){x : (I, Int, Map[Int, Int]) => (x._2, math.abs(x._1.hashCode) % x._3.getOrElse(x._2, 1))}
      .groupBy('rep_bin, 'rep){_.toList[(I, RealVector)]((target_id_field, target_dist_vec_field) -> 'target_list).reducers(1000)}

    val candidates_replicated = candidate_assignments
      .crossWithTiny(bin_replicates)
      .flatMap(('bin, 'bin_replicates) -> ('rep_bin, 'rep)){x : (Int, Map[Int, Int]) => (0 until x._2.getOrElse(x._1, 1)).map{i => (x._1, i)}}
      .project('rep_bin, 'rep, 'candidate_list)

    // Do knn in each cluster, and aggregate.
    candidates_replicated
      .joinWithSmaller(('rep_bin, 'rep) -> ('rep_bin, 'rep), targets_fragmented, new InnerJoin(), 1000)
      .flatMapTo(('target_list, 'candidate_list) -> (target_id_field, neighb_field)){x : (List[(I, RealVector)], List[(J, RealVector)]) =>
        println(x._1.size + " " + x._2.size)
        x._1.view.map{t => (t._1, knn_id[J](t._2, x._2, k))}
      }
      .groupBy(target_id_field){_.reduce[List[(J, Double)]](neighb_field){(a, b) => (a++b).groupBy{_._1}.toList.map{t => (t._1, t._2.map{_._2}.min)}.sortBy{_._2}.take(k)}.reducers(1000)}
      .project(target_id_field, neighb_field)
  }

  def knn2[I,J](targets : Pipe, target_id_field : Symbol, target_vec_field : Symbol,
    candidates : Pipe, candidate_id_field : Symbol, candidate_vec_field : Symbol, neighb_field : Symbol, k : Int,
    init_num_centers : Int = 10000, bins_per_point : Int = 5, max_bin : Int = 5000, min_bin : Int = 50, discard_thresh : Int = 5) : Pipe = {

    knn2_inner[I,J](targets, target_id_field, target_vec_field, target_vec_field,
      candidates, candidate_id_field, candidate_vec_field, candidate_vec_field, neighb_field, k,
      init_num_centers, bins_per_point, max_bin, min_bin, discard_thresh)
  }

  def knn2_rp[I,J](targets : Pipe, target_id_field : Symbol, target_vec_field : Symbol,
    candidates : Pipe, candidate_id_field : Symbol, candidate_vec_field : Symbol, neighb_field : Symbol, k : Int, dim : Int = 50,
    init_num_centers : Int = 10000, bins_per_point : Int = 5, max_bin : Int = 5000, min_bin : Int = 50, discard_thresh : Int = 5) : Pipe = {

    val targets_rp = targets.map(target_vec_field -> 'target_bin_vec){r : RealVector => random_proj(r, dim)}
    val candidates_rp = candidates.map(candidate_vec_field -> 'candidate_bin_vec){r : RealVector => random_proj(r, dim)}

    knn2_inner[I,J](targets_rp, target_id_field, 'target_bin_vec, target_vec_field,
      candidates_rp, candidate_id_field, 'candidate_bin_vec, candidate_vec_field, neighb_field, k,
      init_num_centers, bins_per_point, max_bin, min_bin, discard_thresh)
  }

  // reduction of max product to knn, for finding recommendations from MF type models.
  def max_products[I,J](users : Pipe, user_id_field : Symbol, user_vec_field : Symbol,
    items : Pipe, item_id_field : Symbol, item_vec_field : Symbol, neighb_field : Symbol, k : Int,
    init_num_centers : Int = 10000, bins_per_point : Int = 5, max_bin : Int = 5000, min_bin : Int = 50, discard_thresh : Int = 5) : Pipe = {

    val max_item_norm = items
      .mapTo(item_vec_field -> 'norm){r : RealVector => r.getNorm}
      .groupAll{_.max('norm)}

    val items_pre = items
      .crossWithTiny(max_item_norm)
      .map(('norm, item_vec_field) -> item_vec_field){t : (Double, RealVector) =>
        val scaled = t._2.mapDivideToSelf(t._1)
        val norm = scaled.getNorm
        scaled.append(math.sqrt(1.0 - norm*norm))
      }
    
    val users_pre = users
      .flatMap(user_vec_field -> user_vec_field){r : RealVector =>
        val norm = r.getNorm
        if(math.abs(norm) < 0.000001) {
          None
        } else {
          Some(r.mapDivideToSelf(norm).append(0.0))
        }
      }
  
    knn2_inner[I,J](users_pre, user_id_field, user_vec_field, user_vec_field,
      items_pre, item_id_field, item_vec_field, item_vec_field, neighb_field, k,
      init_num_centers, bins_per_point, max_bin, min_bin, discard_thresh)
  }


}

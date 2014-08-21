package com.etsy.scalding.jobs.conjecture

import com.etsy.conjecture.scalding.NNMF
import com.twitter.scalding.{Args, Job, Tsv, SequenceFile}
import org.apache.commons.math3.linear.RealVector

/*
 * Job to do NNMF of the supplied matrix, given via the arg "A"
 * "alpha" is the extra weight given to non-zero entries.
 */
class NNMFTest(args : Args) extends Job(args) {

  val iter = args.getOrElse("iter", "0").toInt
  val iters = args.getOrElse("iters", "20").toInt
  val base_dir = args.getOrElse("base_dir", "nnmf_test")
  val A_path = args.getOrElse("A", "critics.tsv")
  val alpha = args.getOrElse("alpha", "0.0").toDouble
  
  val A = Tsv(A_path, ('row, 'col, 'val))
    .map('val -> 'val){v : String => v.toDouble}

  val HW = if(iter == 0) {
    // just initialize
    NNMF.initGaussian(A, 10)
  } else {
    // Last iterations output.
    (SequenceFile(base_dir + "/H/" + (iter-1), ('row, 'vec, 'bias)).read,
     SequenceFile(base_dir + "/W/" + (iter-1), ('col, 'vec, 'bias)).read)
  }
  
  val HW_ = NNMF.updateGaussianWeighted(A, HW._1, HW._2, alpha)
  
  HW_._1.write(SequenceFile(base_dir + "/H/" + iter))
  HW_._2.write(SequenceFile(base_dir + "/W/" + iter))

  HW._1.crossWithSmaller(HW._2.rename('vec -> 'vec2).rename('bias -> 'bias2))
    .map(('vec, 'vec2, 'bias, 'bias2) -> 'pred){x : (RealVector, RealVector, Double, Double) => x._1.dotProduct(x._2) + x._3 + x._4}
    .project('row, 'col, 'pred)
    .joinWithSmaller(('row, 'col) -> ('row_, 'col_), A.rename(('row, 'col) -> ('row_, 'col_)), new cascading.pipe.joiner.OuterJoin())
    .mapTo(('val, 'pred) -> 'err){x : (Double, Double) => val d = x._1 - x._2; (if(x._1 == 0.0) 1.0 else (1.0 + alpha)) * d * d}
    .groupAll{_.average('err)}
    .write(Tsv(base_dir+"/err/"+iter))

  // Start more iterations possibly.
  override def next : Option[Job] = {
    val new_args = args + (("iter", Some((iter+1).toString)))
    if(iter < iters - 1) {
      Some(clone(new_args))
    } else {
      None
    }
  }

}

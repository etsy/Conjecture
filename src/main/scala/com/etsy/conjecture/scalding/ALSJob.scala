package com.etsy.conjecture.scalding

import cascading.pipe.Pipe
import cascading.pipe.joiner.InnerJoin
import com.twitter.scalding.{Args, Job, Mode, SequenceFile}
import org.apache.commons.math3.linear._

/**
 * An abstract job class to implement alternating least squares for matrix factorization.
 * Since the method is iterative, this job overrides job.next rather than trying to
 * build a single massive cascading flow.  This means that the job is more robust to failure, and 
 * also doesn't crash the cascading planner with a giant graph.
 *
 * The concrete job class which extends this just has to override the function s() which returns a pipe
 * having fields ('row, 'col, 'value) representing the matrix to factorize.  This is only computed on the
 * first iteration, and then written to disk.  Therefore the function should be self contained, so that the 
 * job doesnt try to do pointless work on every iteration.
 *
 * There are some other fields which the child class can override in order to get specific behavior from
 * the method:
 *
 * - zero_weight: the weight of zeros in the matrix, where nonzeros are given weight 1.
 * - norm_constraint: whether to force the norms of rows of the factors to 1 (useful for doing LSH for max-product search).
 * - lambda_row, lambda_col: L2 regularization parameters on the two factors.
 *
 */

abstract class ALSJob[R, C](args : Args) extends Job(args) {

  override def config(implicit mode : Mode): Map[AnyRef, AnyRef] =
    super.config + ("mapred.child.java.opts" -> "-Xmx3G")

  // Dimension of latent factors.
  val n = args.getOrElse("dim", "200").toInt

  val iter = args.getOrElse("iter", "0").toInt

  val max_iter = args.getOrElse("max_iter", "15").toInt

  val parallelism = args.getOrElse("parallelism", "500").toInt

  val base_dir = args.getOrElse("base_dir", "als")

  // The weight of zero terms in the matrices.
  val zero_weight = 0.001

  // data for s matrix, must have fields ('row, 'col, 'value)
  def s() : Pipe

  def norm_constraint : Boolean = false

  def lambda_row : Double = 0.0f
  def lambda_col : Double = 0.0f

  val incremental = args.boolean("incremental")

  // allow overriding input and output paths.
  val input_u_path = args.getOrElse("input_u_path", base_dir+"/U/"+(iter-1))
  val output_u_path = args.getOrElse("output_u_path", base_dir+"/U/"+iter)
  val output_v_path = args.getOrElse("output_v_path", base_dir+"/V/"+iter)

  // technique to initialize the vector
  def initial_vector(row : R) : RealVector = {
    val rand = new scala.util.Random(row.hashCode)
    val vec = MatrixUtils.createRealVector((0 until n).map{i => rand.nextGaussian}.toArray)
    vec.mapDivide(vec.getNorm)
  }

  val S = if(iter == 0 || args.boolean("update_matrix")) {
    s().project('row, 'col, 'value).write(SequenceFile(base_dir + "/S"))
  } else {
    SequenceFile(base_dir+"/S", ('row, 'col, 'value)).read
  }

  if(iter == 0 && !incremental) {
    // Initial item factors.
    S
      .groupBy('row){_.size('count)}
      .map('row -> 'u_vec)(initial_vector)
      .project('row, 'u_vec)
      .write(SequenceFile(base_dir+"/U/0"))
  } else {
    // Perform iteration of dual alternating least squares.
    val U = SequenceFile(input_u_path, ('row, 'u_vec)).read

    // -- Update V first.
    // Compute U'U
    val UU = U.mapTo('u_vec -> 'UU){u : RealVector => u.outerProduct(u)}
      .groupAll{_.reduce[RealMatrix]('UU){(a, b) => a.add(b)}}

    val V = S.joinWithSmaller('row -> 'row, U, new InnerJoin(), parallelism)
      .groupBy('col){_.toList[(RealVector, Double)](('u_vec, 'value) -> 'u_list).reducers(parallelism).forceToReducers}
      .crossWithTiny(UU)
      .mapTo(('col, 'u_list, 'UU) -> ('col, 'v_vec)){
        x : (C, List[(RealVector, Double)], RealMatrix) =>
        val col_id = x._1
        var XX = x._3.scalarMultiply(zero_weight)
        var Xy = x._2.view.map{t => t._1.mapMultiply(t._2)}.reduce{(a,b) => a.add(b)}.mapMultiply(1.0 + zero_weight)
        x._2.foreach{t => XX = XX.add(t._1.outerProduct(t._1))}
        val lambda = if(norm_constraint) compute_lambda(XX, Xy) else lambda_col
        val res = new LUDecomposition(XX.add(MatrixUtils.createRealIdentityMatrix(XX.getRowDimension).scalarMultiply(lambda))).getSolver.getInverse.operate(Xy)
        (col_id, res)
      }
      .write(SequenceFile(output_v_path))

    // -- Finally update U.
    val VV = V.mapTo('v_vec -> 'VV){u : RealVector => u.outerProduct(u)}
      .groupAll{_.reduce[RealMatrix]('VV){(a, b) => a.add(b)}}

    S
      .joinWithSmaller('col -> 'col, V, new InnerJoin(), parallelism)
      .groupBy('row){_.toList[(RealVector, Double)](('v_vec, 'value) -> 'v_list).reducers(parallelism).forceToReducers}
      .crossWithTiny(VV)
      .mapTo(('row, 'v_list, 'VV) -> ('row, 'u_vec)){
        x : (R, List[(RealVector, Double)], RealMatrix) =>
        val row_id = x._1
        var XX = x._3.scalarMultiply(zero_weight)
        var Xy = x._2.view.map{t => t._1.mapMultiply(t._2)}.reduce{(a,b) => a.add(b)}.mapMultiply(1.0 + zero_weight)
        x._2.foreach{t => XX = XX.add(t._1.outerProduct(t._1))}
        val lambda = if(norm_constraint) compute_lambda(XX, Xy) else lambda_row
        val res = new LUDecomposition(XX.add(MatrixUtils.createRealIdentityMatrix(XX.getRowDimension).scalarMultiply(lambda))).getSolver.getInverse.operate(Xy)
        (row_id, res)
      }
      .write(SequenceFile(output_u_path))
  }

  // for the norm constrained version, compute the lambda necessary so that the output vector has unit norm.
  def compute_lambda(XX : RealMatrix, Xy : RealVector) : Double = {
    val eigen = new EigenDecomposition(XX)
    val u = eigen.getVT.operate(Xy)
    // approximate the lagrange multiplier
    var lambda_max = math.sqrt(u.dotProduct(u))
    var lambda_min = -eigen.getRealEigenvalues.min+0.000000001
    var norm_max = (0 until u.getDimension).map{i => val ui = u.getEntry(i); val ei = eigen.getRealEigenvalue(i); ui*ui / ((ei+lambda_max)*(ei+lambda_max))}.sum
    var norm_min = (0 until u.getDimension).map{i => val ui = u.getEntry(i); val ei = eigen.getRealEigenvalue(i); ui*ui / ((ei+lambda_min)*(ei+lambda_min))}.sum
    while(math.abs(norm_max - norm_min) > 0.0001) {
      val lambda_mid = (lambda_max + lambda_min) / 2.0
      val norm_mid = (0 until u.getDimension).map{i => val ui = u.getEntry(i); val ei = eigen.getRealEigenvalue(i); ui*ui / ((ei+lambda_mid)*(ei+lambda_mid))}.sum
      if(norm_mid < 1) {
        lambda_max = lambda_mid
        norm_max = norm_mid
      } else {
        lambda_min = lambda_mid
        norm_min = norm_mid
      }
    }
    val lambda = (lambda_max + lambda_min) / 2
    lambda
  }

  override def next : Option[Job] = {
    val new_args = args + ("iter", Some((iter+1).toString))
    if(iter < max_iter && !incremental) {
      Some(clone(new_args))
    } else {
      None
    }
  }

}

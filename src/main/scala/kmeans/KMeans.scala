package kmeans

import scala.annotation.tailrec
import scala.collection._
import scala.util.Random
import org.scalameter._
import common._


//The k-means algorithm is very sensitive to the initial choice of means. There are three choice strategies implemented in ScalaShop:
//
//Uniform Choice is the simplest strategy. It chooses n colors uniformly in the entire color space, regardless of the colors used in the image.
// If the image has a dominant color, the means created by this strategy will likely be very far away from the clusters formed by this dominant color.
// You can try setting the Uniform Choice strategy with 1, 10 and 30 steps. You will notice the initial choice is quite bad, but the quality improves as
// the k-means algorithm is applied in more steps.

//Random Sampling is another simple strategy, but with better results. For the initial means, it randomly samples n colors from the image.
// This yields good results if the image has few dominant colors, but it cannot handle subtle nuances in the image. Again, if you try this strategy
// with 1, 10 and 30 k-means iteration steps, you will notice improvements as the k-means algorithm is ran more.

//Uniform Random is the most complex strategy to pick means, but it also produces the best results. It works by uniformly splitting the color space in sub-spaces.
// It then counts the number of pixels that have colors belonging to that sub-space. Based on this number, it chooses a proportional number of means in the
// sub-space, by randomly sampling from the pixels in that sub-space. Therefore, if your image has dominant colors, this strategy will drop a proportional number o
// f means for each dominant color, thus allowing the k-means algorithm to capture fine nuances.

//In the EPFL image now available in ScalaShop, the mountains are a good way to see how well each initial choice of means fares.
// You also have different strategies for deciding convergence:
//
//Steps allows to run a fixed number of steps. After this, the k-means algorithm is stopped.
//Eta corresponds to the means stability, as we showed earlier: if the means did not move much since the last iteration, the result is considered stable.
//Sound-to-noise ratio is a more refined convergence strategy, which does not settle for stability but tries to minimize the difference between the true color image and the index color one. This strategy goes beyond Eta, but high Sound-to-noise ratios will prevent the k-means algorithm from finishing!
//
//

//
//At each iteration of K-means, we can associate multiple points to clusters, and compute the average of the k clusters, in parallel.
// Note that the association of a point to its cluster is independent of the other points in the input, and similarly, the computation of the
// average of a cluster is independent of the other clusters. Once all parallel tasks of the current iteration complete, the algorithm can proceed
// to the next iteration.
//
//K-means a bulk synchronous parallel algorithm (BSP). BSP algorithms are composed from a sequence of supersteps, each of which contains:
//
//parallel computation, in which processes independently perform local computations and produce some values
//communication, in which processes exchange data
//barrier synchronisation, during which processes wait until every process finishes
//Data-parallel programming models are typically a good fit for BSP algorithms, as each bulk synchronous phase can correspond to
// some number of data-parallel operations.
//

class KMeans {

  def generatePoints(k: Int, num: Int): Seq[Point] = {
    val randx = new Random(1)
    val randy = new Random(3)
    val randz = new Random(5)
    (0 until num)
      .map({ i =>
        val x = ((i + 1) % k) * 1.0 / k + randx.nextDouble() * 0.5
        val y = ((i + 5) % k) * 1.0 / k + randy.nextDouble() * 0.5
        val z = ((i + 7) % k) * 1.0 / k + randz.nextDouble() * 0.5
        new Point(x, y, z)
      }).to[mutable.ArrayBuffer]
  }

  def initializeMeans(k: Int, points: Seq[Point]): Seq[Point] = {
    val rand = new Random(7)
    (0 until k).map(_ => points(rand.nextInt(points.length))).to[mutable.ArrayBuffer]
  }

  def findClosest(p: Point, means: GenSeq[Point]): Point = {
    assert(means.size > 0)
    var minDistance = p.squareDistance(means(0))
    var closest = means(0)
    var i = 1
    while (i < means.length) {
      val distance = p.squareDistance(means(i))
      if (distance < minDistance) {
        minDistance = distance
        closest = means(i)
      }
      i += 1
    }
    closest
  }

  def classify(points: GenSeq[Point], means: GenSeq[Point]): GenMap[Point, GenSeq[Point]] = {
    val classifyPointsMapped = points.par.groupBy(findClosest(_, means))
//    println(classifyPointsMapped).toString
    means.par.map(mean => mean -> classifyPointsMapped.getOrElse(mean, GenSeq())).toMap

  }

  def findAverage(oldMean: Point, points: GenSeq[Point]): Point = if (points.length == 0) oldMean else {
    var x = 0.0
    var y = 0.0
    var z = 0.0
    points.seq.foreach { p =>
      x += p.x
      y += p.y
      z += p.z
    }
    new Point(x / points.length, y / points.length, z / points.length)
  }

  def update(classified: GenMap[Point, GenSeq[Point]], oldMeans: GenSeq[Point]): GenSeq[Point] = {
    oldMeans.par.map(oldMean => findAverage(oldMean, classified(oldMean)))
  }

  def converged(eta: Double)(oldMeans: GenSeq[Point], newMeans: GenSeq[Point]): Boolean = {
    (oldMeans zip newMeans).forall{
      case (oldMean, newMean) => oldMean.squareDistance(newMean) < eta
    }
  }

  @tailrec
  final def kMeans(points: GenSeq[Point], means: GenSeq[Point], eta: Double): GenSeq[Point] = {

    val classified = classify(points, means)
    val updatedMeans = update(classified, means)

    if (!converged(eta)(means, updatedMeans)) kMeans(points, updatedMeans, eta) else updatedMeans // your implementation need to be tail recursive
  }
}

/** Describes one point in three-dimensional space.
 *
 *  Note: deliberately uses reference equality.
 */
class Point(val x: Double, val y: Double, val z: Double) {
  private def square(v: Double): Double = v * v
  def squareDistance(that: Point): Double = {
    square(that.x - x)  + square(that.y - y) + square(that.z - z)
  }
  private def round(v: Double): Double = (v * 100).toInt / 100.0
  override def toString = s"(${round(x)}, ${round(y)}, ${round(z)})"
}


object KMeansRunner {

  val standardConfig = config(
    Key.exec.minWarmupRuns -> 20,
    Key.exec.maxWarmupRuns -> 40,
    Key.exec.benchRuns -> 25,
    Key.verbose -> true
  ) withWarmer(new Warmer.Default)

  def main(args: Array[String]) {
    val kMeans = new KMeans()

    val numPoints = 500000
    val eta = 0.01
    val k = 32
    val points = kMeans.generatePoints(k, numPoints)
    val means = kMeans.initializeMeans(k, points)

    val seqtime = standardConfig measure {
      kMeans.kMeans(points, means, eta)
    }
    println(s"sequential time: $seqtime ms")

    val partime = standardConfig measure {
      val parPoints = points.par
      val parMeans = means.par
      kMeans.kMeans(parPoints, parMeans, eta)
    }
    println(s"parallel time: $partime ms")
    println(s"speedup: ${seqtime / partime}")
  }

}

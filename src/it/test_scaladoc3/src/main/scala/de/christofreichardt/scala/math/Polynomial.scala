package de.christofreichardt.scala.math

import de.christofreichardt.diagnosis.{AbstractTracer, TracerFactory}
import de.christofreichardt.scala.diagnosis.Tracing

/**
 * Defines a polynomial in its canonical form.
 *
 * @author Christof Reichardt
 *
 * @constructor
 * @param coefficients the polynoms coefficients
 * @param prime the prime modulus
 */
class Polynomial(
                  val coefficients: IndexedSeq[BigInt],
                  val prime:        BigInt)
  extends Tracing {

  require(prime.isProbablePrime(100))

  /** the remaining coefficients while dropping leading zeros */
  val a: Seq[BigInt] = coefficients.dropWhile(c => c == BigInt(0))
  /** the degree of the polynomial */
  val degree: Int = a.length - 1
  /** indicates the zero polynomial */
  val isZero: Boolean = degree == -1

  /**
   * Computes y = P(x).
   *
   * @param x the x value
   * @return the y value
   */
  def evaluateAt(x: BigInt): BigInt = {
    withTracer("BigInt", this, "evaluateAt(x: BigInt)") {
      val tracer = getCurrentTracer()
      tracer.out().printfIndentln("x = %s", x)
      if (isZero) BigInt(0)
      else {
        Range.inclusive(0, degree)
          .map(i => {
            tracer.out().printfIndentln("a(%d) = %s", degree - i: Integer, a(i))
            (a(i) * x.modPow(degree - i, prime)).mod(prime)
          })
          .foldLeft(BigInt(0))((t0, t1) => (t0 + t1).mod(prime))
      }
    }
  }

  /**
   * Returns a string representation of the Polynomial.
   *
   * @return the string representation of the Polynomial
   */
  override def toString = String.format("Polynomial[a=(%s), degree=%d, isZero=%b, prime=%s]", a.mkString(","), degree: Integer, isZero: java.lang.Boolean, prime)

  override def getCurrentTracer(): AbstractTracer = TracerFactory.getInstance().getDefaultTracer
}
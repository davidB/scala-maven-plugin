/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package xyz.diagnosis

import de.christofreichardt.diagnosis.TracerFactory
import de.christofreichardt.diagnosis.AbstractTracer

/**
 * Contains some add-ons for the <a href="http://www.christofreichardt.de/Projektstudien/TraceLogger/index.html">TraceLogger</a> library.
 *
 * @author Christof Reichardt
 */
trait Tracing {
  /**
   * Custom control structure for tracing of embraced code blocks.
   *
   * @param resultType denotes the return type
   * @param callee     the call site
   * @param method     denotes the method signature
   * @param block      the embraced code block
   * @tparam T the actual type of the embraced code block
   * @return returns whatever block returns
   */
  def withTracer[T](resultType: String, callee: AnyRef, method: String)(block: => T): T = {
    val tracer = getCurrentTracer()
    tracer.entry(resultType, callee, method)
    try {
      block
    }
    finally {
      tracer.wayout()
    }
  }

  /**
   * Returns the present tracer for this object.
   *
   * @return the current tracer, by default the NullTracer
   */
  def getCurrentTracer(): AbstractTracer = TracerFactory.getInstance().getDefaultTracer()
}
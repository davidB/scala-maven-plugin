/*
 * Shamirs Keystore
 *
 * Copyright (C) 2017, 2021, Christof Reichardt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.christofreichardt.scala.diagnosis

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
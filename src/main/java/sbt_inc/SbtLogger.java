
/*
 * Copyright 2011-2020 scala-maven-plugin project (https://davidb.github.io/scala-maven-plugin/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sbt_inc;

import org.apache.maven.plugin.logging.Log;
import sbt.util.Level;
import sbt.util.Logger;
import scala.Enumeration;
import scala.Function0;

public class SbtLogger extends Logger {

  private final Log log;

  SbtLogger(Log l) {
    this.log = l;
  }

  @Override
  public void trace(Function0<Throwable> t) {
    if (log.isDebugEnabled()) {
      log.debug(t.apply());
    }
  }

  @Override
  public void success(Function0<String> message) {
    if (log.isInfoEnabled()) {
      log.info("Success: " + message.apply());
    }
  }

  @Override
  public void log(Enumeration.Value level, Function0<String> message) {
    String s = message.apply();
    String prefix = "[" + level.toString() + "] ";
    if (s.regionMatches(true, 0, prefix, 0, prefix.length())) {
      s = s.substring(prefix.length());
    }
    if (level.equals(Level.Error())) {
      log.error(s);
    } else if (level.equals(Level.Warn())) {
      log.warn(s);
    } else if (level.equals(Level.Info())) {
      log.info(s);
    } else if (level.equals(Level.Debug())) {
      log.debug(s);
    }
  }
}

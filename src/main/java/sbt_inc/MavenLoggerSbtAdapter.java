/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package sbt_inc;

import org.apache.maven.plugin.logging.Log;
import sbt.util.Level;
import sbt.util.Logger;
import scala.Enumeration;
import scala.Function0;

public class MavenLoggerSbtAdapter extends Logger {

  private final Log log;

  MavenLoggerSbtAdapter(Log l) {
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

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
        if (level.equals(Level.Error())) {
            log.error(message.apply());
        } else if (level.equals(Level.Warn())) {
            log.warn(message.apply());
        } else if (level.equals(Level.Info())) {
            log.info(message.apply());
        } else if (level.equals(Level.Debug())) {
            log.debug(message.apply());
        }
    }
}

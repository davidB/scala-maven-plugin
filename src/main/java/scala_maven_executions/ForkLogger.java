/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package scala_maven_executions;

public abstract class ForkLogger {
  private final StringBuilder buffer = new StringBuilder();
  private ForkLogLevel currentLogLevel = null;

  public abstract void onException(Exception t);

  public abstract void onError(String content);

  public abstract void onWarn(String content);

  public abstract void onInfo(String content);

  public abstract void onDebug(String content);

  private void flushBuffer() {
    if (buffer.length() != 0 && currentLogLevel != null) {
      switch (currentLogLevel) {
        case ERROR:
          onError(buffer.toString());
          break;
        case WARN:
          onWarn(buffer.toString());
          break;
        case INFO:
          onInfo(buffer.toString());
          break;
        case DEBUG:
          onDebug(buffer.toString());
          break;
      }
      buffer.setLength(0);
    }
  }

  public final void processLine(String line) {
    try {
      ForkLogLevel newLogLevel = ForkLogLevel.level(line);
      if (newLogLevel != null) {
        flushBuffer();
        currentLogLevel = newLogLevel;
        buffer.append(newLogLevel.removeHeader(line));
      } else {
        buffer.append(System.lineSeparator()).append(line);
      }
    } catch (Exception e) {
      onException(e);
    }
  }

  /**
   * Logs the buffered message, if any. A message is only logged when the next one starts, so this
   * must be called once the forked process output is complete, or the last message is lost.
   */
  public final void flush() {
    try {
      flushBuffer();
    } catch (Exception e) {
      onException(e);
    }
  }
}

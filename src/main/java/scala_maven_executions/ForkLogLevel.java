/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package scala_maven_executions;

public enum ForkLogLevel {
  DEBUG,
  INFO,
  WARN,
  ERROR;

  private final String header;

  ForkLogLevel() {
    this.header = name() + ": ";
  }

  public static ForkLogLevel level(String line) {
    if (line.startsWith(DEBUG.header)) {
      return DEBUG;
    } else if (line.startsWith(INFO.header)) {
      return INFO;
    } else if (line.startsWith(WARN.header)) {
      return WARN;
    } else if (line.startsWith(ERROR.header)) {
      return ERROR;
    }
    return null;
  }

  public String addHeader(String line) {
    return header + line;
  }

  public String removeHeader(String line) {
    return line.substring(header.length());
  }
}

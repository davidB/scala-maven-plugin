/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package scala_maven_executions;

public class LogProcessorUtils {

  public enum Level {
    ERROR,
    WARNING,
    INFO
  }

  public static class LevelState {
    public Level level = Level.INFO;
    public String untilContains = null;
  }

  public static LevelState levelStateOf(String line, LevelState previous) {
    LevelState back = new LevelState();
    String lineLowerCase = line.toLowerCase();
    if (lineLowerCase.contains("error")) {
      back.level = Level.ERROR;
      if (lineLowerCase.contains(".scala")) {
        back.untilContains = "^";
      }
    } else if (lineLowerCase.contains("warn")) {
      back.level = Level.WARNING;
      if (lineLowerCase.contains(".scala")) {
        back.untilContains = "^";
      }
    } else if (previous.untilContains != null) {
      if (!lineLowerCase.contains(previous.untilContains)) {
        back = previous;
      } else {
        back.level = previous.level;
        back.untilContains = null;
      }
    }
    return back;
  }
}

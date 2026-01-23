/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package scala_maven_executions;

import java.util.regex.Pattern;

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

  /** Matches "warning" or "warnings" as complete words, but not filenames e.g. "Warnings.scala". */
  private static final Pattern WARNING_WORD = Pattern.compile("\\bwarnings?\\b(?!\\.scala)");

  /** Matches "error" or "errors" as complete words, but not filenames e.g. "Errors.scala". */
  private static final Pattern ERROR_WORD = Pattern.compile("\\berrors?\\b(?!\\.scala)");

  public static LevelState levelStateOf(String line, LevelState previous) {
    LevelState back = new LevelState();
    String lineLowerCase = line.toLowerCase();
    if (isWarningLine(lineLowerCase)) {
      back.level = Level.WARNING;
      if (lineLowerCase.contains(".scala")) {
        back.untilContains = "^";
      }
    } else if (isErrorLine(lineLowerCase)) {
      back.level = Level.ERROR;
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

  private static boolean isWarningLine(String line) {
    return hasSeverityMarker(line, "warning") || WARNING_WORD.matcher(line).find();
  }

  private static boolean isErrorLine(String line) {
    return hasSeverityMarker(line, "error") || ERROR_WORD.matcher(line).find();
  }

  /**
   * Checks whether the line contains a severity marker e.g. "error:" or "warning:".
   *
   * <p>Matches common compiler and build-tool formats, including:
   *
   * <ul>
   *   <li>{@code path:lineNum: error: message}
   *   <li>{@code error: message} at the start of the line
   * </ul>
   *
   * @param lineLowerCase the lowercase line to check
   * @param marker the severity marker to look for e.g. "error" or "warning"
   * @return true if the line starts with {@code marker:} or contains {@code " marker:"}
   */
  private static boolean hasSeverityMarker(String lineLowerCase, String marker) {
    return lineLowerCase.startsWith(marker + ":") || lineLowerCase.contains(" " + marker + ":");
  }
}

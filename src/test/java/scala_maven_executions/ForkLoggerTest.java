/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package scala_maven_executions;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class ForkLoggerTest {

  private static final class RecordingForkLogger extends ForkLogger {
    final List<String> logs = new ArrayList<>();

    @Override
    public void onException(Exception t) {
      logs.add("exception: " + t);
    }

    @Override
    public void onError(String content) {
      logs.add("error: " + content);
    }

    @Override
    public void onWarn(String content) {
      logs.add("warn: " + content);
    }

    @Override
    public void onInfo(String content) {
      logs.add("info: " + content);
    }

    @Override
    public void onDebug(String content) {
      logs.add("debug: " + content);
    }
  }

  @Test
  public void logsMessageOnlyWhenNextOneStarts() {
    RecordingForkLogger logger = new RecordingForkLogger();
    logger.processLine(ForkLogLevel.INFO.addHeader("first"));
    logger.processLine("continued");
    assertEquals(Collections.emptyList(), logger.logs);

    logger.processLine(ForkLogLevel.ERROR.addHeader("second"));
    assertEquals(
        Collections.singletonList("info: first" + System.lineSeparator() + "continued"),
        logger.logs);
  }

  @Test
  public void flushLogsLastMessage() {
    RecordingForkLogger logger = new RecordingForkLogger();
    logger.processLine(ForkLogLevel.INFO.addHeader("first"));
    logger.processLine(ForkLogLevel.ERROR.addHeader("last"));
    logger.flush();
    assertEquals(Arrays.asList("info: first", "error: last"), logger.logs);

    logger.flush();
    assertEquals(Arrays.asList("info: first", "error: last"), logger.logs);
  }
}

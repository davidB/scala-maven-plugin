
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

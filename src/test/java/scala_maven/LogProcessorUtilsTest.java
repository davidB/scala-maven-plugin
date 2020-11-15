
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
package scala_maven;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import scala_maven_executions.LogProcessorUtils;
import scala_maven_executions.LogProcessorUtils.Level;
import scala_maven_executions.LogProcessorUtils.LevelState;

public class LogProcessorUtilsTest {

  @Test
  public void jdkSplit() throws Exception {
    LevelState previous = new LevelState();
    previous =
        assertLevelState(
            "/home/hub/p/eee/src/main/scala:-1: info: compiling", previous, Level.INFO, null);
    previous =
        assertLevelState(
            "Compiling 128 source files to /home/hub/p/eee/target/classes at 1312794546514",
            previous,
            Level.INFO,
            null);
    previous = assertLevelState("Recompiling 1 files", previous, Level.INFO, null);
    previous =
        assertLevelState(
            "/home/hub/p/eee/src/main/scala/Service.scala:72: error: type mismatch;",
            previous,
            Level.ERROR,
            "^");
    previous = assertLevelState("found : Unit", previous, Level.ERROR, "^");
    previous = assertLevelState("required: () => Any", previous, Level.ERROR, "^");
    previous = assertLevelState("f()", previous, Level.ERROR, "^");
    previous = assertLevelState(" ^", previous, Level.ERROR, null);
    previous =
        assertLevelState(
            "/home/hub/p/eee/src/main/scala/src/main/scala/Service.scala:79: error: type mismatch;",
            previous,
            Level.ERROR,
            "^");
    previous = assertLevelState("found : Unit", previous, Level.ERROR, "^");
    previous = assertLevelState("required: () => Any", previous, Level.ERROR, "^");
    previous = assertLevelState("f()", previous, Level.ERROR, "^");
    previous = assertLevelState("^", previous, Level.ERROR, null);
    previous = assertLevelState("two errors found", previous, Level.ERROR, null);
    previous =
        assertLevelState(
            "------------------------------------------------------------------------",
            previous,
            Level.INFO,
            null);
    previous = assertLevelState("BUILD ERROR", previous, Level.ERROR, null);
    previous =
        assertLevelState(
            "------------------------------------------------------------------------",
            previous,
            Level.INFO,
            null);
    previous =
        assertLevelState(
            "wrap: org.apache.commons.exec.ExecuteException: Process exited with an error: 1(Exit value: 1)",
            previous,
            Level.ERROR,
            null);
  }

  private LevelState assertLevelState(
      String input, LevelState previous, Level expectedLevel, String expectedUntilContains)
      throws Exception {
    LevelState back = LogProcessorUtils.levelStateOf(input, previous);
    assertEquals(expectedLevel, back.level);
    assertEquals(expectedUntilContains, back.untilContains);
    return back;
  }
}

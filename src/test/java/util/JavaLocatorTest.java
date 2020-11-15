
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
package util;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Paths;
import org.apache.maven.toolchain.Toolchain;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

public class JavaLocatorTest {

  @Rule public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Test
  public void shouldReturnNullWhenJavaIsNotAvailableOnCommandLineAndJavaHomeIsPresent() {
    Toolchain toolchain = new NullReturningToolChain();
    environmentVariables.set("JAVA_HOME", "test");
    assertEquals(
        Paths.get("test", "bin", "java").toString(),
        JavaLocator.findExecutableFromToolchain(toolchain));
  }

  @Test
  public void shouldReturnPathToJavaWhenJavaIsPresent() throws Exception {
    Toolchain toolchain = new ReturningToolChain();
    assertEquals("my-path-to-java", JavaLocator.findExecutableFromToolchain(toolchain));
  }

  @Test
  public void shouldThrowExceptionWhenNothingCouldBeFound() {
    Toolchain toolchain = new NullReturningToolChain();
    System.clearProperty("java.home");
    environmentVariables.set("JAVA_HOME", null);
    try {
      JavaLocator.findExecutableFromToolchain(toolchain);
      fail();
    } catch (IllegalStateException e) {
      assertEquals(
          "Couldn't locate java, try setting JAVA_HOME environment variable.", e.getMessage());
    }
  }

  @Test
  public void shouldReturnParentOfChildOfJavaHomeFolder() {
    File home = JavaLocator.findHomeFromToolchain(new TestStringReturningToolChain());
    assertEquals("parent", home.getPath());
  }

  @Test
  public void shouldReturnNullWhenFileIsNotPresent() {
    File home = JavaLocator.findHomeFromToolchain(new ReturningToolChain());
    assertNull(home);
  }

  class NullReturningToolChain implements Toolchain {

    @Override
    public String getType() {
      return null;
    }

    @Override
    public String findTool(String s) {
      return null;
    }
  }

  class TestStringReturningToolChain implements Toolchain {

    @Override
    public String getType() {
      return null;
    }

    @Override
    public String findTool(String s) {
      return "parent/child/my-path-to-java";
    }
  }

  class ReturningToolChain implements Toolchain {

    @Override
    public String getType() {
      return null;
    }

    @Override
    public String findTool(String s) {
      return "my-path-to-java";
    }
  }
}

/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package util;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import org.apache.maven.toolchain.Toolchain;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

public class JavaLocatorTest {

  @Rule public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Test
  public void shouldReturnNotNullWhenJavaIsNotAvailableOnCommandLineAndJavaHomeIsPresent() throws IOException {
    Toolchain toolchain = new ReturningToolChain(null);
    assertNotNull(JavaLocator.findExecutableFromToolchain(toolchain));
  }

  @Test
  public void shouldReturnPathToJavaWhenJavaIsPresent() throws Exception {
    Toolchain toolchain = new ReturningToolChain("my-path-to-java");
    assertEquals("my-path-to-java", JavaLocator.findExecutableFromToolchain(toolchain));
  }

  @Test
  public void shouldThrowExceptionWhenNothingCouldBeFound() throws IOException {
    Toolchain toolchain = new ReturningToolChain(null);
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
  public void shouldReturnParentOfChildOfJavaHomeFolder() throws IOException {
    File home = JavaLocator.findHomeFromToolchain(new ReturningToolChain("parent/child/my-path-to-java"));
    assertEquals("parent", home.getPath());
  }

  @Test
  public void shouldReturnNullWhenFileIsNotPresent() throws IOException {
    File home = JavaLocator.findHomeFromToolchain(new ReturningToolChain("my-path-to-java"));
    assertNull(home);
  }

  static final class ReturningToolChain implements Toolchain {
    private final String tool;

    ReturningToolChain(String tool) {
      this.tool = tool;
    }

    @Override
    public String getType() {
      return null;
    }

    @Override
    public String findTool(String toolName) {
      return tool;
    }
  }
}

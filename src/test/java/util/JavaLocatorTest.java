/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package util;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.maven.toolchain.Toolchain;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import uk.org.webcompere.systemstubs.rules.EnvironmentVariablesRule;

public class JavaLocatorTest {

  @Rule public final TemporaryFolder tempFolder = new TemporaryFolder();
  @Rule public final EnvironmentVariablesRule environmentVariables = new EnvironmentVariablesRule();

  private String originalJavaHome;

  @Before
  public void saveJavaHome() {
    originalJavaHome = System.getProperty("java.home");
  }

  @After
  public void restoreJavaHome() {
    if (originalJavaHome != null) {
      System.setProperty("java.home", originalJavaHome);
    } else {
      System.clearProperty("java.home");
    }
  }

  @Test
  public void shouldReturnNotNullWhenJavaIsNotAvailableOnCommandLineAndJavaHomeIsPresent() {
    Toolchain toolchain = new ReturningToolChain(null);
    assertNotNull(JavaLocator.findExecutableFromToolchain(toolchain));
  }

  @Test
  public void shouldReturnPathToJavaWhenJavaIsPresent() {
    Toolchain toolchain = new ReturningToolChain("my-path-to-java");
    assertEquals("my-path-to-java", JavaLocator.findExecutableFromToolchain(toolchain).getPath());
  }

  @Test
  public void shouldThrowExceptionWhenNothingCouldBeFound() {
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
  public void shouldReturnParentOfChildOfJavaHomeFolder() {
    File home =
        JavaLocator.findHomeFromToolchain(new ReturningToolChain("parent/child/my-path-to-java"));
    assertEquals("parent", home.getPath());
  }

  @Test
  public void shouldReturnNullWhenFileIsNotPresent() {
    File home = JavaLocator.findHomeFromToolchain(new ReturningToolChain("my-path-to-java"));
    assertNull(home);
  }

  @Test
  public void shouldFindJavaFromJavaHomeWhenToolchainIsNull() throws IOException {
    // Create a fake JDK structure under temp folder
    Path javaHome = tempFolder.newFolder("fakejdk").toPath();
    Path javaExec = javaHome.resolve("bin").resolve("java");
    Files.createDirectories(javaExec.getParent());
    Files.createFile(javaExec);

    System.setProperty("java.home", javaHome.toString());
    environmentVariables.set("JAVA_HOME", null);

    File result = JavaLocator.findExecutableFromToolchain(null);
    assertNotNull(result);
    assertEquals(javaExec.toString(), result.getAbsolutePath());
  }

  @Test
  public void shouldFindJavaInJreSiblingBinWhenJavaHomeEndsWithJre() throws IOException {
    // Create structure: fakejdk/jre (java.home) and fakejdk/bin/java (sibling)
    Path jdkRoot = tempFolder.newFolder("fakejdk").toPath();
    Path jreDir = jdkRoot.resolve("jre");
    Files.createDirectories(jreDir);
    Path javaExec = jdkRoot.resolve("bin").resolve("java");
    Files.createDirectories(javaExec.getParent());
    Files.createFile(javaExec);

    System.setProperty("java.home", jreDir.toString());
    environmentVariables.set("JAVA_HOME", null);

    File result = JavaLocator.findExecutableFromToolchain(null);
    assertNotNull(result);
    assertEquals(javaExec.toString(), result.getAbsolutePath());
  }

  @Test
  public void shouldFallBackToJreBinWhenSiblingBinDoesNotExist() throws IOException {
    // Create structure: fakejdk/jre/bin/java (java.home ends with jre, but no sibling bin)
    Path jdkRoot = tempFolder.newFolder("fakejdk").toPath();
    Path jreDir = jdkRoot.resolve("jre");
    Path javaExec = jreDir.resolve("bin").resolve("java");
    Files.createDirectories(javaExec.getParent());
    Files.createFile(javaExec);

    System.setProperty("java.home", jreDir.toString());
    environmentVariables.set("JAVA_HOME", null);

    File result = JavaLocator.findExecutableFromToolchain(null);
    assertNotNull(result);
    assertEquals(javaExec.toString(), result.getAbsolutePath());
  }

  @Test
  public void shouldThrowWhenJavaHomeDoesNotContainJava() throws IOException {
    // Create a java.home without bin/java
    Path javaHome = tempFolder.newFolder("fakejdk").toPath();
    Files.createDirectories(javaHome.resolve("bin"));

    System.setProperty("java.home", javaHome.toString());
    environmentVariables.set("JAVA_HOME", null);

    try {
      JavaLocator.findExecutableFromToolchain(null);
      fail("Expected IllegalStateException");
    } catch (IllegalStateException e) {
      assertEquals("Couldn't locate java in defined java.home system property.", e.getMessage());
    }
  }

  @Test
  public void shouldFindJavaFromJavaHomeEnvVar() throws IOException {
    // Create a fake JDK structure under temp folder
    Path javaHome = tempFolder.newFolder("fakejdk").toPath();
    Path javaExec = javaHome.resolve("bin").resolve("java");
    Files.createDirectories(javaExec.getParent());
    Files.createFile(javaExec);

    System.clearProperty("java.home");
    environmentVariables.set("JAVA_HOME", javaHome.toString());

    File result = JavaLocator.findExecutableFromToolchain(null);
    assertNotNull(result);
    assertEquals(javaExec.toString(), result.getAbsolutePath());
  }

  @Test
  public void shouldThrowWhenJavaHomeEnvVarDoesNotContainJava() throws IOException {
    // Create a JAVA_HOME without bin/java
    Path javaHome = tempFolder.newFolder("fakejdk").toPath();
    Files.createDirectories(javaHome.resolve("bin"));

    System.clearProperty("java.home");
    environmentVariables.set("JAVA_HOME", javaHome.toString());

    try {
      JavaLocator.findExecutableFromToolchain(null);
      fail("Expected IllegalStateException");
    } catch (IllegalStateException e) {
      assertEquals(
          "Couldn't locate java in defined JAVA_HOME environment variable.", e.getMessage());
    }
  }

  @Test
  public void shouldReturnCorrectPathFromJavaHomeProperty() throws IOException {
    // Verify the returned path contains the expected java.home prefix
    Path javaHome = tempFolder.newFolder("myjdk").toPath();
    Path javaExec = javaHome.resolve("bin").resolve("java");
    Files.createDirectories(javaExec.getParent());
    Files.createFile(javaExec);

    System.setProperty("java.home", javaHome.toString());
    environmentVariables.set("JAVA_HOME", null);

    File result = JavaLocator.findExecutableFromToolchain(null);
    assertTrue(result.getAbsolutePath().startsWith(javaHome.toString()));
    assertEquals("java", result.getName());
  }

  @Test
  public void shouldNotEnterJreBranchWhenJavaHomeDoesNotEndWithJre() throws IOException {
    // Create structure where a sibling "bin/java" exists but java.home doesn't end with "jre"
    // If the endsWith("jre") check is always true (mutant), it would return the sibling path
    // instead of the correct path inside java.home
    Path parentDir = tempFolder.newFolder("parent").toPath();
    Path javaHome = parentDir.resolve("myjdk");
    Files.createDirectories(javaHome);

    // Create the CORRECT java inside java.home/bin/java
    Path correctJava = javaHome.resolve("bin").resolve("java");
    Files.createDirectories(correctJava.getParent());
    Files.createFile(correctJava);

    // Create a DECOY java at sibling path: parent/bin/java (resolveSibling("bin") from myjdk)
    Path decoyJava = parentDir.resolve("bin").resolve("java");
    Files.createDirectories(decoyJava.getParent());
    Files.createFile(decoyJava);

    System.setProperty("java.home", javaHome.toString());
    environmentVariables.set("JAVA_HOME", null);

    File result = JavaLocator.findExecutableFromToolchain(null);
    assertNotNull(result);
    // Must return the java inside java.home, NOT the sibling decoy
    assertEquals(correctJava.toString(), result.getAbsolutePath());
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

/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package scala_maven;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;
import scala_maven_executions.JavaMainCallerSupport;
import scala_maven_executions.SpawnMonitor;

public class ScalaMojoSupportTest {

  @Test
  public void scala2_11_should_generate_prefixed_target() {
    assertEquals(
        asList("-target:jvm-1.5"),
        ScalaMojoSupport.computeBytecodeVersionOptions("1.5", null, new VersionNumber("2.11.12")));
    assertEquals(
        asList("-target:jvm-1.5"),
        ScalaMojoSupport.computeBytecodeVersionOptions("5", null, new VersionNumber("2.11.12")));
    assertEquals(
        asList("-target:jvm-1.6"),
        ScalaMojoSupport.computeBytecodeVersionOptions("1.6", null, new VersionNumber("2.11.12")));
    assertEquals(
        asList("-target:jvm-1.6"),
        ScalaMojoSupport.computeBytecodeVersionOptions("6", null, new VersionNumber("2.11.12")));
    assertEquals(
        asList("-target:jvm-1.7"),
        ScalaMojoSupport.computeBytecodeVersionOptions("1.7", null, new VersionNumber("2.11.12")));
    assertEquals(
        asList("-target:jvm-1.7"),
        ScalaMojoSupport.computeBytecodeVersionOptions("7", null, new VersionNumber("2.11.12")));
    assertEquals(
        asList("-target:jvm-1.8"),
        ScalaMojoSupport.computeBytecodeVersionOptions("1.8", null, new VersionNumber("2.11.12")));
    assertEquals(
        asList("-target:jvm-1.8"),
        ScalaMojoSupport.computeBytecodeVersionOptions("8", null, new VersionNumber("2.11.12")));
  }

  @Test
  public void scala2_11_should_generate_nothing_for_unsupported_java_versions() {
    assertTrue(
        ScalaMojoSupport.computeBytecodeVersionOptions("11", null, new VersionNumber("2.11.12"))
            .isEmpty());
    assertTrue(
        ScalaMojoSupport.computeBytecodeVersionOptions("17", null, new VersionNumber("2.11.12"))
            .isEmpty());
  }

  @Test
  public void scala2_12_should_generate_release() {
    assertEquals(
        asList("-release", "8"),
        ScalaMojoSupport.computeBytecodeVersionOptions("1.8", null, new VersionNumber("2.12.11")));
    assertEquals(
        asList("-release", "8"),
        ScalaMojoSupport.computeBytecodeVersionOptions("1.8", "8", new VersionNumber("2.12.11")));
    assertEquals(
        asList("-release", "11"),
        ScalaMojoSupport.computeBytecodeVersionOptions("1.8", "11", new VersionNumber("2.12.11")));
    assertEquals(
        asList("-release", "17"),
        ScalaMojoSupport.computeBytecodeVersionOptions("1.8", "17", new VersionNumber("2.12.11")));
  }

  @Test
  public void scala2_13_should_generate_release() {
    assertEquals(
        asList("-release", "8"),
        ScalaMojoSupport.computeBytecodeVersionOptions("1.8", null, new VersionNumber("2.13.10")));
    assertEquals(
        asList("-release", "8"),
        ScalaMojoSupport.computeBytecodeVersionOptions("1.8", "8", new VersionNumber("2.13.10")));
    assertEquals(
        asList("-release", "11"),
        ScalaMojoSupport.computeBytecodeVersionOptions("1.8", "11", new VersionNumber("2.13.10")));
    assertEquals(
        asList("-release", "17"),
        ScalaMojoSupport.computeBytecodeVersionOptions("1.8", "17", new VersionNumber("2.13.10")));
  }

  @Test
  public void scala3_1_1_should_generate_release() {
    assertEquals(
        asList("-release", "8"),
        ScalaMojoSupport.computeBytecodeVersionOptions("1.8", null, new VersionNumber("3.1.1")));
    assertEquals(
        asList("-release", "8"),
        ScalaMojoSupport.computeBytecodeVersionOptions("1.8", "8", new VersionNumber("3.1.1")));
    assertEquals(
        asList("-release", "11"),
        ScalaMojoSupport.computeBytecodeVersionOptions("1.8", "11", new VersionNumber("3.1.1")));
    assertEquals(
        asList("-release", "17"),
        ScalaMojoSupport.computeBytecodeVersionOptions("1.8", "17", new VersionNumber("3.1.1")));
  }

  @Test
  public void scala3_1_2_should_generate_java_output_version() {
    assertEquals(
        asList("-java-output-version", "8"),
        ScalaMojoSupport.computeBytecodeVersionOptions("1.8", null, new VersionNumber("3.1.2")));
    assertEquals(
        asList("-java-output-version", "8"),
        ScalaMojoSupport.computeBytecodeVersionOptions("1.8", "8", new VersionNumber("3.1.2")));
    assertEquals(
        asList("-java-output-version", "11"),
        ScalaMojoSupport.computeBytecodeVersionOptions("1.8", "11", new VersionNumber("3.1.2")));
    assertEquals(
        asList("-java-output-version", "17"),
        ScalaMojoSupport.computeBytecodeVersionOptions("1.8", "17", new VersionNumber("3.1.2")));
  }

  static class ScalaMojoSupportWithRelease extends ScalaMojoSupport {
    public ScalaMojoSupportWithRelease() {
      this.release = "42";
      this.target = "8";
    }

    public List<String> getScalacOptions() throws Exception {
      return super.getScalacOptions();
    }

    public void setScalaVersion(final String v) {
      this.scalaVersion = v;
    }

    @Override
    protected void doExecute() throws Exception {}
  }

  static class JavaMainCallerArgs extends JavaMainCallerSupport {
    public JavaMainCallerArgs() {
      super(null, null, null, null, null);
    }

    public List<String> getArgs() {
      return this.args;
    }

    @Override
    public SpawnMonitor spawn(boolean displayCmd) throws Exception {
      return null;
    }

    @Override
    public boolean run(boolean displayCmd, boolean throwFailure) throws Exception {
      return false;
    }

    @Override
    public void redirectToLog() {}
  }

  final ScalaMojoSupportWithRelease mojoWithRelease = new ScalaMojoSupportWithRelease();

  @Test
  public void scala2_11_should_skip_release_option() throws Exception {
    mojoWithRelease.setScalaVersion("2.11.0");
    List<String> opts = mojoWithRelease.getScalacOptions();
    assertNotNull(opts);
    assertFalse(opts.contains("-release"));
    assertFalse(opts.contains("42"));
  }

  @Test
  public void scala2_12_should_skip_release_option() throws Exception {
    mojoWithRelease.setScalaVersion("2.12.0");
    List<String> opts = mojoWithRelease.getScalacOptions();
    assertNotNull(opts);
    assertTrue(opts.contains("-release"));
    assertTrue(opts.contains("42"));
  }

  @Test
  public void scala2_13_should_keep_release_option() throws Exception {
    mojoWithRelease.setScalaVersion("2.13.0");
    List<String> opts = mojoWithRelease.getScalacOptions();
    assertNotNull(opts);
    assertTrue(opts.contains("-release"));
    assertTrue(opts.contains("42"));
  }

  @Test
  public void scala2_12_scala_command_prefers_release_over_target() throws Exception {
    mojoWithRelease.setScalaVersion("2.12.0");
    final JavaMainCallerArgs caller = new JavaMainCallerArgs();
    mojoWithRelease.populateArgs(caller);

    assertNotNull(caller.getArgs());
    assertEquals(2, caller.getArgs().size());
    assertTrue(caller.getArgs().contains("-release"));
    assertTrue(caller.getArgs().contains("42"));
  }
}

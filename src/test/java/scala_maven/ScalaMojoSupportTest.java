/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package scala_maven;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;
import scala_maven_executions.JavaMainCaller;
import scala_maven_executions.JavaMainCallerSupport;

public class ScalaMojoSupportTest {

  @Test
  public void scala2_11_should_generate_prefixed_target() {
    assertEquals("jvm-1.5", ScalaMojoSupport.targetOption("1.5", new VersionNumber("2.11.12")));
    assertEquals("jvm-1.5", ScalaMojoSupport.targetOption("5", new VersionNumber("2.11.12")));
    assertEquals("jvm-1.6", ScalaMojoSupport.targetOption("1.6", new VersionNumber("2.11.12")));
    assertEquals("jvm-1.6", ScalaMojoSupport.targetOption("6", new VersionNumber("2.11.12")));
    assertEquals("jvm-1.7", ScalaMojoSupport.targetOption("1.7", new VersionNumber("2.11.12")));
    assertEquals("jvm-1.7", ScalaMojoSupport.targetOption("7", new VersionNumber("2.11.12")));
    assertEquals("jvm-1.8", ScalaMojoSupport.targetOption("1.8", new VersionNumber("2.11.12")));
    assertEquals("jvm-1.8", ScalaMojoSupport.targetOption("8", new VersionNumber("2.11.12")));
  }

  @Test
  public void scala2_11_should_generate_null_for_unsupported_java_versions() {
    assertNull(ScalaMojoSupport.targetOption("11", new VersionNumber("2.11.12")));
    assertNull(ScalaMojoSupport.targetOption("17", new VersionNumber("2.11.12")));
  }

  @Test
  public void scala2_12_should_generate_prefixed_target() {
    assertEquals("jvm-1.5", ScalaMojoSupport.targetOption("1.5", new VersionNumber("2.12.11")));
    assertEquals("jvm-1.5", ScalaMojoSupport.targetOption("5", new VersionNumber("2.12.11")));
    assertEquals("jvm-1.6", ScalaMojoSupport.targetOption("1.6", new VersionNumber("2.12.11")));
    assertEquals("jvm-1.6", ScalaMojoSupport.targetOption("6", new VersionNumber("2.12.11")));
    assertEquals("jvm-1.7", ScalaMojoSupport.targetOption("1.7", new VersionNumber("2.12.11")));
    assertEquals("jvm-1.7", ScalaMojoSupport.targetOption("7", new VersionNumber("2.12.11")));
    assertEquals("jvm-1.8", ScalaMojoSupport.targetOption("1.8", new VersionNumber("2.12.11")));
    assertEquals("jvm-1.8", ScalaMojoSupport.targetOption("8", new VersionNumber("2.12.11")));
  }

  @Test
  public void scala2_12_should_generate_null_for_unsupported_java_versions() {
    assertNull(ScalaMojoSupport.targetOption("11", new VersionNumber("2.12.11")));
    assertNull(ScalaMojoSupport.targetOption("17", new VersionNumber("2.12.11")));
  }

  @Test
  public void scala2_13_should_generate_non_prefixed_target() {
    assertEquals("5", ScalaMojoSupport.targetOption("1.5", new VersionNumber("2.13.8")));
    assertEquals("5", ScalaMojoSupport.targetOption("5", new VersionNumber("2.13.8")));
    assertEquals("6", ScalaMojoSupport.targetOption("1.6", new VersionNumber("2.13.8")));
    assertEquals("6", ScalaMojoSupport.targetOption("6", new VersionNumber("2.13.8")));
    assertEquals("7", ScalaMojoSupport.targetOption("1.7", new VersionNumber("2.13.8")));
    assertEquals("7", ScalaMojoSupport.targetOption("7", new VersionNumber("2.13.8")));
    assertEquals("8", ScalaMojoSupport.targetOption("1.8", new VersionNumber("2.13.8")));
    assertEquals("8", ScalaMojoSupport.targetOption("8", new VersionNumber("2.13.8")));
    assertEquals("11", ScalaMojoSupport.targetOption("11", new VersionNumber("2.13.8")));
    assertEquals("17", ScalaMojoSupport.targetOption("17", new VersionNumber("2.13.8")));
  }

  static class ScalaMojoSupportWithRelease extends ScalaMojoSupport {
    public ScalaMojoSupportWithRelease() {
      this.release = "42";
      this.target = "8";
    }

    public List<String> getScalacOptions() throws Exception {
      try {
        return super.getScalacOptions();
      } catch (final Exception e) {
        e.printStackTrace();
        throw e;
      }
    }

    public void setScalaVersion(final String v) {
      this.scalaVersion = v;
    }

    @Override
    protected void doExecute() throws Exception {}

    @Override
    JavaMainCaller getEmptyScalaCommand(final String mainClass, final boolean forkOverride) {
      return new JavaMainCallerArgs(this);
    }
  }

  static class JavaMainCallerArgs extends JavaMainCallerSupport {
    public JavaMainCallerArgs(final ScalaMojoSupport mojo) {
      super(mojo, null, null, null, null);
    }

    public List<String> getArgs() {
      return this.args;
    }

    @Override
    public scala_maven_executions.SpawnMonitor spawn(boolean displayCmd) throws Exception {
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
  public void scala2_12_scala_command_contain_target_and_release() throws Exception {
    mojoWithRelease.setScalaVersion("2.12.0");
    final JavaMainCaller caller = mojoWithRelease.getScalaCommand(true, "String");
    assertNotNull(caller);
    assertTrue(caller instanceof JavaMainCallerArgs);

    final JavaMainCallerArgs callerJvm = (JavaMainCallerArgs) caller;
    assertNotNull(callerJvm.getArgs());
    assertEquals(3, callerJvm.getArgs().size());
    assertTrue(callerJvm.getArgs().contains("-release"));
    assertTrue(callerJvm.getArgs().contains("42"));
    assertTrue(callerJvm.getArgs().contains("-target:jvm-1.8"));
  }
}

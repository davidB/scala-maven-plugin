/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package scala_maven;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

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
}

/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package scala_maven;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

import org.junit.Test;

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
}

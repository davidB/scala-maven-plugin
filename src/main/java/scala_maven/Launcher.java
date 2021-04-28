/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package scala_maven;

import org.apache.maven.plugins.annotations.Parameter;

public class Launcher {
  String id;

  String mainClass;

  /** Jvm Arguments */
  @Parameter protected String[] jvmArgs;

  /** compiler additional arguments */
  @Parameter protected String[] args;
}

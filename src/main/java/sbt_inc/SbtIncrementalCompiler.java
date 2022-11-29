/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package sbt_inc;

import java.io.File;
import java.util.Collection;

public interface SbtIncrementalCompiler {

  void compile(
      Collection<File> classpathElements,
      Collection<File> sources,
      File classesDirectory,
      Collection<String> scalacOptions,
      Collection<String> javacOptions);
}

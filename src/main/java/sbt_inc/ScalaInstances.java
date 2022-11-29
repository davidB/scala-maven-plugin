/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package sbt_inc;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import sbt.internal.inc.ScalaInstance;
import scala.Option;
import scala_maven.ScalaCompilerLoader;

public final class ScalaInstances {

  static ScalaInstance makeScalaInstance(
      String scalaVersion,
      Collection<File> compilerAndDependencies,
      Collection<File> libraryAndDependencies) {

    URL[] compilerJarUrls = toUrls(compilerAndDependencies);
    URL[] libraryJarUrls = toUrls(libraryAndDependencies);

    SortedSet<File> allJars = new TreeSet<>();
    allJars.addAll(compilerAndDependencies);
    allJars.addAll(libraryAndDependencies);

    ClassLoader loaderLibraryOnly =
        new ScalaCompilerLoader(libraryJarUrls, xsbti.Reporter.class.getClassLoader());
    ClassLoader loaderCompilerOnly = new URLClassLoader(compilerJarUrls, loaderLibraryOnly);

    return new ScalaInstance(
        scalaVersion,
        loaderCompilerOnly,
        loaderCompilerOnly,
        loaderLibraryOnly,
        libraryAndDependencies.toArray(new File[] {}),
        compilerAndDependencies.toArray(new File[] {}),
        allJars.toArray(new File[] {}),
        Option.apply(scalaVersion));
  }

  private static URL[] toUrls(Collection<File> files) {
    return files.stream()
        .map(
            x -> {
              try {
                return x.toURI().toURL();
              } catch (MalformedURLException e) {
                throw new RuntimeException("failed to convert into url " + x, e);
              }
            })
        .toArray(URL[]::new);
  }
}

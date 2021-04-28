/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package scala_maven_dependency;

import java.util.Arrays;
import java.util.List;

public class ArtifactIds4Scala2 implements ArtifactIds {
  static final String SCALA_LIBRARY_ARTIFACTID = "scala-library";
  static final String SCALA_COMPILER_ARTIFACTID = "scala-compiler";
  static List<String> SCALA_DISTRO_ARTIFACTS =
      Arrays.asList(
          SCALA_LIBRARY_ARTIFACTID,
          "scala-swing",
          "scala-dbc",
          SCALA_COMPILER_ARTIFACTID,
          "scalap",
          "partest");

  @Override
  public List<String> scalaDistroArtifactIds() throws Exception {
    return SCALA_DISTRO_ARTIFACTS;
  }

  @Override
  public String scalaLibraryArtifactId() throws Exception {
    return SCALA_LIBRARY_ARTIFACTID;
  }

  @Override
  public String scalaCompilerArtifactId() throws Exception {
    return SCALA_COMPILER_ARTIFACTID;
  }
}

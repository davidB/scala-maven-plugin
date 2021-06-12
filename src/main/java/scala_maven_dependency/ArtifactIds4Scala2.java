/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package scala_maven_dependency;

import java.util.Arrays;
import java.util.List;
import scala_maven.VersionNumber;

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

  public String compilerMainClassName(boolean useFsc) throws Exception {
    return useFsc ? "scala.tools.nsc.CompileClient" : "scala.tools.nsc.Main";
  }

  public String consoleMainClassName() throws Exception {
    return "scala.tools.nsc.MainGenericRunner";
  }

  public String apidocMainClassName(VersionNumber sv) throws Exception {
    boolean isPreviousScala271 = (new VersionNumber("2.7.1").compareTo(sv) > 0 && !sv.isZero());
    if (!isPreviousScala271) {
      return "scala.tools.nsc.ScalaDoc";
    } else {
      return "scala.tools.nsc.Main";
    }
  }
}

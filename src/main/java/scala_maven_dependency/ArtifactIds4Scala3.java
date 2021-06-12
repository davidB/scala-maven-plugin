/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package scala_maven_dependency;

import java.util.Arrays;
import java.util.List;
import scala_maven.VersionNumber;

public class ArtifactIds4Scala3 implements ArtifactIds {
  protected static final String SCALA_LIBRARY_ARTIFACTID = "scala3-library";
  protected static final String SCALA_COMPILER_ARTIFACTID = "scala3-compiler";
  static final List<String> SCALA_DISTRO_ARTIFACTS =
      Arrays.asList(
          SCALA_LIBRARY_ARTIFACTID, "scala3-swing", "scala3-dbc", SCALA_COMPILER_ARTIFACTID);

  private VersionNumber scalaVersion;

  public ArtifactIds4Scala3(VersionNumber scalaVersion) {
    this.scalaVersion = scalaVersion;
  }

  private String getScala3ArtifactId(String a) {
    return a + "_" + getBinaryVersionForScala3();
  }

  private String getBinaryVersionForScala3() {
    return scalaVersion.major == 3
            && scalaVersion.minor == 0
            && scalaVersion.bugfix == 0
            && scalaVersion.modifier != null
        ? scalaVersion.toString()
        : "3";
  }

  @Override
  public List<String> scalaDistroArtifactIds() throws Exception {
    return SCALA_DISTRO_ARTIFACTS;
  }

  @Override
  public String scalaLibraryArtifactId() throws Exception {
    return getScala3ArtifactId(SCALA_LIBRARY_ARTIFACTID);
  }

  @Override
  public String scalaCompilerArtifactId() throws Exception {
    return getScala3ArtifactId(SCALA_COMPILER_ARTIFACTID);
  }

  public String compilerMainClassName(boolean useFsc) throws Exception {
    return "dotty.tools.dotc.Main";
  }

  public String consoleMainClassName() throws Exception {
    // return "dotty.tools.dotc.Run";
    return "dotty.tools.repl.Main";
  }

  public String apidocMainClassName(VersionNumber sv) throws Exception {
    return "dotty.tools.dotc.Main";
  }
}


/*
 * Copyright 2011-2020 scala-maven-plugin project (https://davidb.github.io/scala-maven-plugin/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
}

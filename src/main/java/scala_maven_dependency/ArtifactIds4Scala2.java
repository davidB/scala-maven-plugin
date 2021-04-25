
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

public class ArtifactIds4Scala2 implements ArtifactIds {
  static final String SCALA_LIBRARY_ARTIFACTID = "scala-library";
  static final String SCALA_REFLECT_ARTIFACTID = "scala-reflect";
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
  public String scalaReflectArtifactId() throws Exception {
    return SCALA_REFLECT_ARTIFACTID;
  }

  @Override
  public String scalaCompilerArtifactId() throws Exception {
    return SCALA_COMPILER_ARTIFACTID;
  }
}

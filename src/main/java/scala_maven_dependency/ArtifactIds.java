
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

import java.util.List;
import java.util.regex.Pattern;

public interface ArtifactIds {
  static Pattern SCALA_LIBRARY_PATTERN = Pattern.compile("scala([0-9]?)-library");

  List<String> scalaDistroArtifactIds() throws Exception;

  String scalaLibraryArtifactId() throws Exception;

  String scalaCompilerArtifactId() throws Exception;
}

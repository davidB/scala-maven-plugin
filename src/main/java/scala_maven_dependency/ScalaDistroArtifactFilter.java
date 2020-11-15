
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

import static scala_maven_dependency.ScalaConstants.*;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.filter.DependencyNodeFilter;

/**
 * A filter to only look at scala distribution maven artifacts.
 *
 * @author JSuereth
 */
public class ScalaDistroArtifactFilter implements DependencyNodeFilter, ArtifactFilter {
  private final String scalaOrganization;

  public ScalaDistroArtifactFilter(String scalaOrganization) {
    this.scalaOrganization = scalaOrganization;
  }

  @Override
  public boolean include(Artifact artifact) {
    // TODO - Are we checking the right artifacts?
    return scalaOrganization.equalsIgnoreCase(artifact.getGroupId())
        && SCALA_DISTRO_ARTIFACTS.contains(artifact.getArtifactId());
  }

  @Override
  public boolean accept(DependencyNode node) {
    return include(node.getArtifact());
  }
}

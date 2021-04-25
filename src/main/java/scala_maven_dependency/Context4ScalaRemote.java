
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

import java.io.File;
import java.util.Set;
import org.apache.maven.artifact.Artifact;
import scala_maven.MavenArtifactResolver;
import scala_maven.VersionNumber;

public class Context4ScalaRemote implements Context {
  private final String scalaOrganization;
  private final VersionNumber scalaVersion;
  private final VersionNumber scalaCompatVersion;
  //  private final ArtifactService artifactService;
  private final ArtifactIds aids;
  private final MavenArtifactResolver mavenArtifactResolver;

  public Context4ScalaRemote(
      VersionNumber scalaVersion,
      VersionNumber scalaVersionCompat,
      ArtifactIds aids,
      String scalaOrganization,
      MavenArtifactResolver mavenArtifactResolver) {
    this.scalaOrganization = scalaOrganization;
    this.scalaVersion = scalaVersion;
    this.scalaCompatVersion = scalaVersionCompat;
    this.aids = aids;
    this.mavenArtifactResolver = mavenArtifactResolver;
  }

  @Override
  public boolean hasInDistro(Artifact artifact) throws Exception {
    return scalaOrganization.equalsIgnoreCase(artifact.getGroupId())
        && aids.scalaDistroArtifactIds().contains(artifact.getArtifactId());
  }

  @Override
  public VersionNumber version() {
    return scalaVersion;
  }

  @Override
  public VersionNumber versionCompat() {
    return scalaCompatVersion;
  }

  private File findJarFile(String artifactId) throws Exception {
    return mavenArtifactResolver
        .getJar(scalaOrganization, artifactId, scalaVersion.toString(), "")
        .getFile();
  }

  @Override
  public File findLibraryJar() throws Exception {
    return findJarFile(aids.scalaLibraryArtifactId());
  }

  @Override
  public File findReflectJar() throws Exception {
    return findJarFile(aids.scalaReflectArtifactId());
  }

  @Override
  public File findCompilerJar() throws Exception {
    return findJarFile(aids.scalaCompilerArtifactId());
  }

  @Override
  public Set<Artifact> findCompilerAndDependencies() throws Exception {
    return mavenArtifactResolver.getJarAndDependencies(
        scalaOrganization, aids.scalaCompilerArtifactId(), scalaVersion.toString(), null);
  }
}

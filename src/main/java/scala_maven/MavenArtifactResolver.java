
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
package scala_maven;

import java.util.NoSuchElementException;
import java.util.Set;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.repository.RepositorySystem;

public class MavenArtifactResolver {

  private final RepositorySystem repositorySystem;
  private final MavenSession session;

  public MavenArtifactResolver(RepositorySystem repositorySystem, MavenSession session) {
    this.repositorySystem = repositorySystem;
    this.session = session;
  }

  public Artifact getJar(String groupId, String artifactId, String version, String classifier) {
    Artifact artifact = createJarArtifact(groupId, artifactId, version, classifier);
    Set<Artifact> resolvedArtifacts = resolve(artifact, false);
    if (resolvedArtifacts.isEmpty()) {
      throw new NoSuchElementException(
          String.format(
              "Could not resolve artifact %s:%s:%s:%s", groupId, artifactId, version, classifier));
    }
    return resolvedArtifacts.iterator().next();
  }

  public Set<Artifact> getJarAndDependencies(
      String groupId, String artifactId, String version, String classifier) {
    Artifact artifact = createJarArtifact(groupId, artifactId, version, classifier);
    return resolve(artifact, true);
  }

  private Artifact createJarArtifact(
      String groupId, String artifactId, String version, String classifier) {
    return classifier == null
        ? repositorySystem.createArtifact(groupId, artifactId, version, ScalaMojoSupport.JAR)
        : repositorySystem.createArtifactWithClassifier(
            groupId, artifactId, version, ScalaMojoSupport.JAR, classifier);
  }

  private Set<Artifact> resolve(Artifact artifact, boolean transitively) {
    ArtifactResolutionRequest request =
        new ArtifactResolutionRequest()
            .setArtifact(artifact)
            .setResolveRoot(true)
            .setResolveTransitively(transitively)
            .setServers(session.getRequest().getServers())
            .setMirrors(session.getRequest().getMirrors())
            .setProxies(session.getRequest().getProxies())
            .setLocalRepository(session.getLocalRepository())
            .setRemoteRepositories(session.getCurrentProject().getRemoteArtifactRepositories());
    return repositorySystem.resolve(request).getArtifacts();
  }
}

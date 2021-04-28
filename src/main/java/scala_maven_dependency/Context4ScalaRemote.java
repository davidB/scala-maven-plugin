/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package scala_maven_dependency;

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

  @Override
  public Set<Artifact> findLibraryAndDependencies() throws Exception {
    return mavenArtifactResolver.getJarAndDependencies(
        scalaOrganization, aids.scalaLibraryArtifactId(), scalaVersion.toString(), null);
  }

  @Override
  public Set<Artifact> findCompilerAndDependencies() throws Exception {
    return mavenArtifactResolver.getJarAndDependencies(
        scalaOrganization, aids.scalaCompilerArtifactId(), scalaVersion.toString(), null);
  }
}

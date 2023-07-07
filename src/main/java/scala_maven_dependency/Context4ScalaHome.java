/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package scala_maven_dependency;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;
import scala_maven.MavenArtifactResolver;
import scala_maven.VersionNumber;

public class Context4ScalaHome extends ContextBase implements Context {
  private final File scalaHome;

  public Context4ScalaHome(
      VersionNumber scalaVersion,
      VersionNumber scalaCompatVersion,
      ArtifactIds aids,
      File scalaHome) {
    super(scalaVersion, scalaCompatVersion, aids);
    this.scalaHome = scalaHome;
  }

  @Override
  public boolean hasInDistro(Artifact artifact) throws Exception {
    return false;
  }

  @Override
  public Set<Artifact> findLibraryAndDependencies() throws Exception {
    File lib = new File(scalaHome, "lib");
    File f = new File(lib, aids.scalaLibraryArtifactId() + ".jar");
    Set<Artifact> d = new TreeSet<>();
    d.add(
        new LocalFileArtifact("local", aids.scalaLibraryArtifactId(), scalaVersion.toString(), f));
    return d;
  }

  @Override
  public Set<Artifact> findCompilerAndDependencies() throws Exception {
    //        String compiler = aids.scalaCompilerArtifactId();
    Set<Artifact> d = new TreeSet<>();
    for (File f : new File(scalaHome, "lib").listFiles()) {
      String name = f.getName();
      if (name.endsWith(".jar")) {
        d.add(
            new LocalFileArtifact(
                "local", name.substring(0, name.length() - 4), scalaVersion.toString(), f));
      }
    }
    return d;
  }

  @Override
  public Set<Artifact> findScalaDocAndDependencies() throws Exception {
    return Collections.emptySet();
  }
}

class LocalFileArtifact implements Artifact {
  private String groupId;
  private String artifactId;
  private String version;
  private File file;

  public LocalFileArtifact(String groupId, String artifactId, String version, File file) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
    this.file = file;
  }

  @Override
  public String getGroupId() {
    return groupId;
  }

  @Override
  public String getArtifactId() {
    return artifactId;
  }

  @Override
  public String getVersion() {
    return version;
  }

  @Override
  public void setVersion(String version) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public String getScope() {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public String getType() {
    return MavenArtifactResolver.JAR;
  }

  @Override
  public String getClassifier() {
    return null;
  }

  @Override
  public boolean hasClassifier() {
    return false;
  }

  @Override
  public File getFile() {
    return file;
  }

  @Override
  public void setFile(File destination) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public String getBaseVersion() {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void setBaseVersion(String baseVersion) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public String getId() {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public String getDependencyConflictId() {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void addMetadata(ArtifactMetadata metadata) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public Collection<ArtifactMetadata> getMetadataList() {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void setRepository(ArtifactRepository remoteRepository) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public ArtifactRepository getRepository() {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void updateVersion(String version, ArtifactRepository localRepository) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public String getDownloadUrl() {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void setDownloadUrl(String downloadUrl) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public ArtifactFilter getDependencyFilter() {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void setDependencyFilter(ArtifactFilter artifactFilter) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public ArtifactHandler getArtifactHandler() {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public List<String> getDependencyTrail() {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void setDependencyTrail(List<String> dependencyTrail) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void setScope(String scope) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public VersionRange getVersionRange() {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void setVersionRange(VersionRange newRange) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void selectVersion(String version) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void setGroupId(String groupId) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void setArtifactId(String artifactId) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public boolean isSnapshot() {
    return false;
  }

  @Override
  public void setResolved(boolean resolved) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public boolean isResolved() {
    return true;
  }

  @Override
  public void setResolvedVersion(String version) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void setArtifactHandler(ArtifactHandler handler) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public boolean isRelease() {
    return false;
  }

  @Override
  public void setRelease(boolean release) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public List<ArtifactVersion> getAvailableVersions() {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void setAvailableVersions(List<ArtifactVersion> versions) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public boolean isOptional() {
    return false;
  }

  @Override
  public void setOptional(boolean optional) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public ArtifactVersion getSelectedVersion() throws OverConstrainedVersionException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public boolean isSelectedVersionKnown() throws OverConstrainedVersionException {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public int compareTo(Artifact o) {
    int c = groupId.compareTo(o.getGroupId());
    if (c != 0) return c;
    c = artifactId.compareTo(o.getArtifactId());
    if (c != 0) return c;
    // TODO compare on classifier ?
    return version.compareTo(o.getVersion());
  }
}

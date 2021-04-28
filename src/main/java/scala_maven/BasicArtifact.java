/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package scala_maven;

public class BasicArtifact {
  public String groupId;
  public String artifactId;
  public String version;
  public String classifier;

  @Override
  public String toString() {
    return "BasicArtifact(" + groupId + "," + artifactId + "," + version + "," + classifier + ")";
  }
}

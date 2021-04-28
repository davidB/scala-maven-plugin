/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package scala_maven_dependency;

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
  private final Context scalaContext;

  public ScalaDistroArtifactFilter(Context scalaContext) {
    this.scalaContext = scalaContext;
  }

  @Override
  public boolean include(Artifact artifact) {
    try {
      return scalaContext.hasInDistro(artifact);
    } catch (Exception exc) {
      exc.printStackTrace();
      return false;
    }
  }

  @Override
  public boolean accept(DependencyNode node) {
    return include(node.getArtifact());
  }
}

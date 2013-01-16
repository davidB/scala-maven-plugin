package scala_maven_dependency;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.filter.DependencyNodeFilter;

import static scala_maven_dependency.ScalaConstants.*;
/**
 * A filter to only look at scala distribution maven artifacts.
 * 
 * @author JSuereth
 */
public class ScalaDistroArtifactFilter implements DependencyNodeFilter, ArtifactFilter {
	@Override
  public boolean include(Artifact artifact) {
		//TODO - Are we checking the right artifacts?
		return SCALA_DISTRO_GROUP.equalsIgnoreCase(artifact.getGroupId()) && 
		SCALA_DISTRO_ARTIFACTS.contains(artifact.getArtifactId());
	}
	@Override
  public boolean accept(DependencyNode node) {
		return include(node.getArtifact());
	}

}

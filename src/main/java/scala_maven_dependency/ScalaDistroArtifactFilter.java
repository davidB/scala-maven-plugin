package scala_maven_dependency;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.filter.DependencyNodeFilter;

import static scala_maven_dependency.ScalaConstants.*;
/**
 * A filter to only look at scala distribution maven artifacts.
 * 
 * @author JSuereth
 */
public class ScalaDistroArtifactFilter implements DependencyNodeFilter, ArtifactFilter {
    private final String scalaOrganization;

    public ScalaDistroArtifactFilter(String scalaOrganization) {
        this.scalaOrganization=scalaOrganization;
    }

    @Override
  public boolean include(Artifact artifact) {
		//TODO - Are we checking the right artifacts?
		return scalaOrganization.equalsIgnoreCase(artifact.getGroupId()) &&
		SCALA_DISTRO_ARTIFACTS.contains(artifact.getArtifactId());
	}
	@Override
  public boolean accept(DependencyNode node) {
		return include(node.getArtifact());
	}

}

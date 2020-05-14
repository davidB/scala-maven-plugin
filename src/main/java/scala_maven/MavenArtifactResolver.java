package scala_maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.repository.RepositorySystem;

import java.util.NoSuchElementException;
import java.util.Set;

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
                String.format("Could not resolve artifact %s:%s:%s:%s", groupId, artifactId, version, classifier));
        }
        return resolvedArtifacts.iterator().next();
    }

    public Set<Artifact> getJarAndDependencies(String groupId, String artifactId, String version, String classifier) {
        Artifact artifact = createJarArtifact(groupId, artifactId, version, classifier);
        return resolve(artifact, true);
    }

    private Artifact createJarArtifact(String groupId, String artifactId, String version, String classifier) {
        return classifier == null ? repositorySystem.createArtifact(groupId, artifactId, version, ScalaMojoSupport.JAR)
            : repositorySystem.createArtifactWithClassifier(groupId, artifactId, version, ScalaMojoSupport.JAR,
                classifier);
    }

    private Set<Artifact> resolve(Artifact artifact, boolean transitively) {
        ArtifactResolutionRequest request = new ArtifactResolutionRequest() //
            .setArtifact(artifact) //
            .setResolveRoot(true) //
            .setResolveTransitively(transitively) //
            .setServers(session.getRequest().getServers()) //
            .setMirrors(session.getRequest().getMirrors()) //
            .setProxies(session.getRequest().getProxies()) //
            .setLocalRepository(session.getLocalRepository()) //
            .setRemoteRepositories(session.getCurrentProject().getRemoteArtifactRepositories());
        return repositorySystem.resolve(request).getArtifacts();
    }
}

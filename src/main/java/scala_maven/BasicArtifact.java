package scala_maven;

public class BasicArtifact {
    public String groupId;
    public String artifactId;
    public String version;

    @Override
    public String toString() {
        return "BasicArtifact(" + groupId + "," + artifactId + "," + version + ")";
    }
}

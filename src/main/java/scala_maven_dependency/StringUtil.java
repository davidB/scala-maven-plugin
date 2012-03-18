package scala_maven_dependency;

import org.apache.maven.artifact.Artifact;

/**
 * Utilities for making Error messages.
 */
public class StringUtil {

    /**
     * Creates a human-readable string for an artifact.
     * @param artifact
     * @return
     */
    public static String makeArtifactNameString(Artifact artifact) {
        //TODO - Handle version ranges...
        if(artifact == null) {
            return "<null artifact>";
        }
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
    }
}

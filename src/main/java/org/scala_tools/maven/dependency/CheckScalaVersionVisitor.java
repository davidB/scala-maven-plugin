package org.scala_tools.maven.dependency;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor;
import org.scala_tools.maven.VersionNumber;
import static org.scala_tools.maven.dependency.ScalaConstants.*;
/**
 * Ensures that all scala versions match the given version.
 * @author JSuereth
 *
 */
public class CheckScalaVersionVisitor implements DependencyNodeVisitor {
    private VersionNumber _version;
    private boolean _failed = false;
    private Log _log;

    public boolean endVisit(DependencyNode node) {
        return !_failed;
    }

    public CheckScalaVersionVisitor(String projectVerison, Log log) {
        this._version = new VersionNumber(projectVerison);
        this._log = log;
    }

    public boolean isScalaDistroArtifact(Artifact artifact) {
        return SCALA_DISTRO_GROUP.equalsIgnoreCase(artifact.getGroupId()) &&
        SCALA_DISTRO_ARTIFACTS.contains(artifact.getArtifactId());
    }
    public boolean visit(DependencyNode node) {
        //TODO - Do we care about provided scope?
        Artifact artifact = node.getArtifact();
        _log.debug("checking ["+artifact+"] for scala version");
        //TODO - Handle version ranges???? does that make sense given scala's binary incompatability!
        if(isScalaDistroArtifact(artifact) && artifact.getVersion() != null) {
            VersionNumber originalVersion = new VersionNumber(artifact.getVersion());
            if(originalVersion.compareTo(_version) != 0) {
                _failed = true;
            }
        } else {
            //TODO - What now?
        }
        return !_failed;
    }

    public boolean isFailed() {
        return _failed;
    }
}

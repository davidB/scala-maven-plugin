
/*
 * Copyright 2011-2020 scala-maven-plugin project (https://davidb.github.io/scala-maven-plugin/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package scala_maven_dependency;

import static scala_maven_dependency.ScalaConstants.*;

import java.util.ArrayList;
import java.util.List;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.traversal.DependencyNodeVisitor;
import scala_maven.VersionNumber;

/**
 * Ensures that all scala versions match the given version.
 *
 * @author JSuereth
 */
public class CheckScalaVersionVisitor implements DependencyNodeVisitor {
  private VersionNumber _version;
  private boolean _failed = false;
  private Log _log;
  private String _scalaOrganization;

  private List<String> scalaDependentArtifactStrings = new ArrayList<>();

  @Override
  public boolean endVisit(DependencyNode node) {
    return !_failed;
  }

  public CheckScalaVersionVisitor(VersionNumber version, Log log, String scalaOrganization) {
    this._version = version;
    this._log = log;
    this._scalaOrganization = scalaOrganization;
  }

  private boolean isScalaDistroArtifact(Artifact artifact) {
    if (_version.major == 2 || _version.major == 3) {
      return _scalaOrganization.equalsIgnoreCase(artifact.getGroupId())
          && (SCALA_DISTRO_ARTIFACTS.contains(artifact.getArtifactId())
              || SCALA3_DISTRO_ARTIFACTS.contains(artifact.getArtifactId()));
    } else {
      return _scalaOrganization.equalsIgnoreCase(artifact.getGroupId());
    }
  }

  @Override
  public boolean visit(DependencyNode node) {
    // TODO - Do we care about provided scope?
    Artifact artifact = node.getArtifact();
    _log.debug("checking [" + artifact + "] for scala version");
    // TODO - Handle version ranges???? does that make sense given scala's binary
    // incompatability!
    if (isScalaDistroArtifact(artifact) && artifact.getVersion() != null) {
      VersionNumber originalVersion = new VersionNumber(artifact.getVersion());
      if (_version.compareTo(originalVersion) != 0) { // _version can be a VersionNumberMask
        _failed = true;
      }
      // If this dependency is transitive, we want to track which artifact requires
      // this...
      if (node.getParent()
          != null) { // TODO - Go all the way up the parent chain till we hit the bottom....
        final Artifact parentArtifact = node.getParent().getArtifact();
        scalaDependentArtifactStrings.add(
            " "
                + StringUtil.makeArtifactNameString(parentArtifact)
                + " requires scala version: "
                + originalVersion);
      }
    } else {
      // TODO - What now?
    }
    return !_failed;
  }

  public boolean isFailed() {
    return _failed;
  }

  public void logScalaDependents() {
    _log.warn(" Expected all dependencies to require Scala version: " + _version);
    for (String dependString : scalaDependentArtifactStrings) {
      _log.warn(dependString);
    }
  }
}

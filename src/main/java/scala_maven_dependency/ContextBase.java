/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package scala_maven_dependency;

import org.codehaus.plexus.util.StringUtils;
import scala_maven.VersionNumber;

abstract class ContextBase implements Context {
  protected final VersionNumber scalaVersion;
  protected final VersionNumber scalaCompatVersion;
  protected final ArtifactIds aids;

  protected ContextBase(
      VersionNumber scalaVersion, VersionNumber scalaCompatVersion, ArtifactIds aids) {
    this.scalaVersion = scalaVersion;
    this.scalaCompatVersion = scalaCompatVersion;
    this.aids = aids;
  }

  @Override
  public VersionNumber version() {
    return scalaVersion;
  }

  @Override
  public VersionNumber versionCompat() {
    return scalaCompatVersion;
  }

  @Override
  public String compilerMainClassName(String override, boolean useFsc) throws Exception {
    if (StringUtils.isEmpty(override)) {
      return this.aids.compilerMainClassName(useFsc);
    } else {
      return override;
    }
  }

  @Override
  public String consoleMainClassName(String override) throws Exception {
    if (StringUtils.isEmpty(override)) {
      return this.aids.consoleMainClassName();
    } else {
      return override;
    }
  }

  @Override
  public String apidocMainClassName(String override) throws Exception {
    if (StringUtils.isEmpty(override)) {
      return this.aids.apidocMainClassName(this.scalaVersion);
    } else {
      return override;
    }
  }
}

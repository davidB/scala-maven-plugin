/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package scala_maven_dependency;

import java.util.Set;
import org.apache.maven.artifact.Artifact;
import scala_maven.VersionNumber;

public interface Context {
  boolean hasInDistro(Artifact artifact) throws Exception;

  VersionNumber version();

  VersionNumber versionCompat();

  Set<Artifact> findLibraryAndDependencies() throws Exception;

  Set<Artifact> findCompilerAndDependencies() throws Exception;

  Set<Artifact> findScalaDocAndDependencies() throws Exception;

  String compilerMainClassName(String override, boolean useFsc) throws Exception;

  String consoleMainClassName(String override) throws Exception;

  String apidocMainClassName(String override) throws Exception;
}

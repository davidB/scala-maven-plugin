/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package scala_maven_dependency;

import java.util.List;
import java.util.regex.Pattern;

public interface ArtifactIds {
  static Pattern SCALA_LIBRARY_PATTERN = Pattern.compile("scala([0-9]?)-library");

  List<String> scalaDistroArtifactIds() throws Exception;

  String scalaLibraryArtifactId() throws Exception;

  String scalaCompilerArtifactId() throws Exception;

  String scalaDocArtifactId() throws Exception;

  String compilerMainClassName(boolean useFsc) throws Exception;

  String consoleMainClassName() throws Exception;

  String apidocMainClassName() throws Exception;
}

/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package scala_maven;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugins.annotations.Parameter;
import scala_maven_executions.MainHelper;
import util.FileUtils;

/** @author david.bernard */
public abstract class ScalaSourceMojoSupport extends ScalaMojoSupport {

  /** Enables/Disables sending java source to the scala compiler. */
  @Parameter(defaultValue = "true")
  protected boolean sendJavaToScalac = true;

  /**
   * A list of inclusion filters for the compiler. ex :
   *
   * <pre>
   *    &lt;includes&gt;
   *      &lt;include&gt;SomeFile.scala&lt;/include&gt;
   *    &lt;/includes&gt;
   * </pre>
   */
  @Parameter protected Set<String> includes = new HashSet<>();

  /**
   * A list of exclusion filters for the compiler. ex :
   *
   * <pre>
   *    &lt;excludes&gt;
   *      &lt;exclude&gt;SomeBadFile.scala&lt;/exclude&gt;
   *    &lt;/excludes&gt;
   * </pre>
   */
  @Parameter private Set<String> excludes = new HashSet<>();

  /**
   * Additional dependencies to be added to the classpath. This can be useful in situations where a
   * dependency is needed at compile time, but should not be treated as a dependency in the
   * published POM.
   */
  @Parameter(property = "additionalDependencies")
  private Dependency[] additionalDependencies;

  /**
   * Retrieves the list of *all* root source directories. We need to pass all .java and .scala files
   * into the scala compiler
   */
  protected abstract List<File> getSourceDirectories() throws Exception;

  private boolean _filterPrinted = false;

  /** Finds all source files in a set of directories with a given extension. */
  List<File> findSourceWithFilters() throws Exception {
    return findSourceWithFilters(getSourceDirectories());
  }

  private void initFilters() throws Exception {
    if (includes.isEmpty()) {
      includes.add("**/*.scala");
      if (sendJavaToScalac && isJavaSupportedByCompiler()) {
        includes.add("**/*.java");
      }
    }
    if (!_filterPrinted && getLog().isDebugEnabled()) {
      StringBuilder builder = new StringBuilder("includes = [");
      for (String include : includes) {
        builder.append(include).append(",");
      }
      builder.append("]");
      getLog().debug(builder.toString());

      builder = new StringBuilder("excludes = [");
      for (String exclude : excludes) {
        builder.append(exclude).append(",");
      }
      builder.append("]");
      getLog().debug(builder.toString());
      _filterPrinted = true;
    }
  }

  /** Finds all source files in a set of directories with a given extension. */
  List<File> findSourceWithFilters(List<File> sourceRootDirs) throws Exception {
    List<File> sourceFiles = new ArrayList<>();

    initFilters();

    // TODO - Since we're making files anyway, perhaps we should just test
    // for existence here...
    for (File dir : sourceRootDirs) {
      String[] tmpFiles =
          MainHelper.findFiles(
              dir, includes.toArray(new String[] {}), excludes.toArray(new String[] {}));
      for (String tmpLocalFile : tmpFiles) {
        File tmpAbsFile = FileUtils.fileOf(new File(dir, tmpLocalFile), useCanonicalPath);
        sourceFiles.add(tmpAbsFile);
      }
    }
    // scalac is sensitive to scala file order, file system can't guarantee file
    // order => unreproducible build error across platforms
    // sort files by path (OS dependent) to guarantee reproducible command line.
    Collections.sort(sourceFiles);
    return sourceFiles;
  }

  /** This limits the source directories to only those that exist for real. */
  List<File> normalize(List<String> compileSourceRootsList) throws Exception {
    List<File> newCompileSourceRootsList = new ArrayList<>();
    if (compileSourceRootsList != null) {
      // copy as I may be modifying it
      for (String srcDir : compileSourceRootsList) {
        File srcDirFile = FileUtils.fileOf(new File(srcDir), useCanonicalPath);
        if (!newCompileSourceRootsList.contains(srcDirFile) && srcDirFile.exists()) {
          newCompileSourceRootsList.add(srcDirFile);
        }
      }
    }
    return newCompileSourceRootsList;
  }

  protected void addAdditionalDependencies(Set<File> back) throws Exception {
    if (additionalDependencies != null) {
      for (Dependency dependency : additionalDependencies) {
        addToClasspath(
            dependency.getGroupId(),
            dependency.getArtifactId(),
            dependency.getVersion(),
            dependency.getClassifier(),
            back,
            false);
      }
    }
  }
}

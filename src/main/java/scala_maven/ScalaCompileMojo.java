/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package scala_maven;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import util.FileUtils;

/**
 * Compiles a directory of Scala source. Corresponds roughly to the compile goal of the
 * maven-compiler-plugin
 */
@Mojo(
    name = "compile",
    defaultPhase = LifecyclePhase.COMPILE,
    requiresDependencyResolution = ResolutionScope.COMPILE,
    threadSafe = true)
public class ScalaCompileMojo extends ScalaCompilerSupport {

  /**
   * Set this to 'true' to bypass compilation of main sources. Its use is NOT RECOMMENDED, but quite
   * convenient on occasion.
   */
  @Parameter(property = "maven.main.skip")
  private boolean skipMain;

  /** The directory in which to place compilation output */
  @Parameter(property = "outputDir", defaultValue = "${project.build.outputDirectory}")
  private File outputDir;

  /** The directory which contains scala/java source files */
  @Parameter(defaultValue = "${project.build.sourceDirectory}/../scala")
  private File sourceDir;

  /** Analysis cache file for incremental recompilation. */
  @Parameter(
      property = "analysisCacheFile",
      defaultValue = "${project.build.directory}/analysis/compile")
  private File analysisCacheFile;

  /**
   * List of directories or jars to add to the classpath. @Deprecated Use {@code
   * additionalDependencies} instead.
   */
  @Parameter(property = "classpath")
  @Deprecated
  private Classpath classpath;

  @Override
  protected List<File> getSourceDirectories() throws Exception {
    List<String> sources = project.getCompileSourceRoots();
    String scalaSourceDir = FileUtils.pathOf(sourceDir, useCanonicalPath);
    if (!sources.contains(scalaSourceDir)) {
      sources = new LinkedList<>(sources); // clone the list to keep the original unmodified
      sources.add(scalaSourceDir);
    }
    return normalize(sources);
  }

  @Override
  protected Set<File> getClasspathElements() throws Exception {
    final Set<File> back = FileUtils.fromStrings(project.getCompileClasspathElements());
    addAdditionalDependencies(back);
    if (classpath != null && classpath.getAdd() != null) {
      getLog().warn("using 'classpath' is deprecated, use 'additionalDependencies' instead");
      for (File f : classpath.getAdd()) {
        back.add(f);
      }
    }
    back.addAll(FileUtils.fromStrings(TychoUtilities.addOsgiClasspathElements(project)));
    return back;
  }

  @Override
  @Deprecated
  protected List<Dependency> getDependencies() {
    return project.getCompileDependencies();
  }

  @Override
  protected File getOutputDir() {
    return outputDir.getAbsoluteFile();
  }

  @Override
  protected File getAnalysisCacheFile() {
    return analysisCacheFile.getAbsoluteFile();
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skipMain) {
      getLog().info("Not compiling main sources");
      return;
    }
    super.execute();
  }
}

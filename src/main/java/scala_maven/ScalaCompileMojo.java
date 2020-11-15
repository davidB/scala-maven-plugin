
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
package scala_maven;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

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
  protected Set<String> getClasspathElements() throws Exception {
    final Set<String> back = new HashSet<>(project.getCompileClasspathElements());
    back.remove(project.getBuild().getOutputDirectory());
    addAdditionalDependencies(back);
    if (classpath != null && classpath.getAdd() != null) {
      getLog().warn("using 'classpath' is deprecated, use 'additionalDependencies' instead");
      for (File f : classpath.getAdd()) {
        back.add(f.getAbsolutePath());
      }
    }
    back.addAll(TychoUtilities.addOsgiClasspathElements(project));
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
}

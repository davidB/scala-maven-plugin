
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
 * Compile Scala test source into test-classes. Corresponds roughly to testCompile in
 * maven-compiler-plugin
 */
@Mojo(
    name = "testCompile",
    defaultPhase = LifecyclePhase.TEST_COMPILE,
    requiresDependencyResolution = ResolutionScope.TEST,
    threadSafe = true)
public class ScalaTestCompileMojo extends ScalaCompilerSupport {

  /**
   * Set this to 'true' to bypass unit tests entirely. Its use is NOT RECOMMENDED, but quite
   * convenient on occasion.
   */
  @Parameter(property = "maven.test.skip")
  private boolean skip;

  /** The directory in which to place test compilation output */
  @Parameter(defaultValue = "${project.build.testOutputDirectory}")
  private File testOutputDir;

  /** The directory in which to find test scala source code */
  @Parameter(defaultValue = "${project.build.testSourceDirectory}/../scala")
  private File testSourceDir;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skip) {
      return;
    }
    super.execute();
  }

  /** Analysis cache file for incremental recompilation. */
  @Parameter(
      property = "testAnalysisCacheFile",
      defaultValue = "${project.build.directory}/analysis/test-compile")
  private File testAnalysisCacheFile;

  @Override
  protected Set<File> getClasspathElements() throws Exception {
    Set<File> back = FileUtils.fromStrings(project.getTestClasspathElements());
    back.remove(project.getBuild().getTestOutputDirectory());
    addAdditionalDependencies(back);
    return back;
  }

  @Override
  protected List<Dependency> getDependencies() {
    return project.getTestDependencies();
  }

  @Override
  protected File getOutputDir() {
    return testOutputDir.getAbsoluteFile();
  }

  @Override
  protected List<File> getSourceDirectories() throws Exception {
    List<String> sources = project.getTestCompileSourceRoots();
    String scalaSourceDir = testSourceDir.getAbsolutePath();
    if (!sources.contains(scalaSourceDir)) {
      sources.add(scalaSourceDir);
    }
    return normalize(sources);
  }

  @Override
  protected File getAnalysisCacheFile() {
    return testAnalysisCacheFile.getAbsoluteFile();
  }
}

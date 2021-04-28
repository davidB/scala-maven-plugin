
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
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import util.FileUtils;

/** Add more source directories to the POM. */
@Mojo(
    name = "add-source",
    executionStrategy = "always",
    defaultPhase = LifecyclePhase.INITIALIZE,
    threadSafe = true)
public class AddSourceMojo extends AbstractMojo {

  /** The maven project */
  @Parameter(property = "project", required = true, readonly = true)
  private MavenProject project;

  /** The directory in which scala source is found */
  @Parameter(defaultValue = "${project.build.sourceDirectory}/../scala")
  private File sourceDir;

  /** The directory in which testing scala source is found */
  @Parameter(defaultValue = "${project.build.testSourceDirectory}/../scala")
  private File testSourceDir;

  /**
   * Should use CanonicalPath to normalize path (true =&gt; getCanonicalPath, false =&gt;
   * getAbsolutePath)
   *
   * @see <a href="https://github.com/davidB/maven-scala-plugin/issues/50">#50</a>
   */
  @Parameter(property = "maven.scala.useCanonicalPath", defaultValue = "true")
  private boolean useCanonicalPath;

  @Override
  public void execute() {
    try {
      if (sourceDir != null) {
        final String path = FileUtils.pathOf(sourceDir, useCanonicalPath);
        if (!project.getCompileSourceRoots().contains(path)) {
          getLog().info("Add Source directory: " + path);
          project.addCompileSourceRoot(path);
        }
      }
      if (testSourceDir != null) {
        final String path = FileUtils.pathOf(testSourceDir, useCanonicalPath);
        if (!project.getTestCompileSourceRoots().contains(path)) {
          getLog().info("Add Test Source directory: " + path);
          project.addTestCompileSourceRoot(path);
        }
      }
    } catch (final Exception exc) {
      getLog().warn(exc);
    }
  }
}

/*
 * Copyright 2007 scala-tools.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */
 package org_scala_tools_maven;

import java.io.File;
import java.util.List;

import org.apache.maven.model.Dependency;

/**
 * Compiles a directory of Scala source. Corresponds roughly to the compile goal
 * of the maven-compiler-plugin
 *
 * @phase compile
 * @goal compile
 * @requiresDependencyResolution compile
 */
public class ScalaCompileMojo extends ScalaCompilerSupport {

    /**
     * The directory in which to place compilation output
     *
     * @parameter expression="${project.build.outputDirectory}"
     */
    protected File outputDir;

    /**
     * The directory which contains scala/java source files
     *
     * @parameter expression="${project.build.sourceDirectory}/../scala"
     */
    protected File sourceDir;
    
    @Override
    @SuppressWarnings("unchecked")
    protected List<File> getSourceDirectories() throws Exception {
        List<String> sources = project.getCompileSourceRoots();
        //Quick fix in case the user has not added the "add-source" goal.
        String scalaSourceDir = sourceDir.getCanonicalPath();
        if(!sources.contains(scalaSourceDir)) {
            sources.add(scalaSourceDir);
        }
        return normalize(sources);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<String> getClasspathElements() throws Exception {
        return TychoUtilities.addOsgiClasspathElements(project, project.getCompileClasspathElements());
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<Dependency> getDependencies() {
        return project.getCompileDependencies();
    }

    @Override
    protected File getOutputDir() throws Exception {
        return outputDir.getAbsoluteFile();
    }
}

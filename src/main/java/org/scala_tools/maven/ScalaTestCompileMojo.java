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
package org.scala_tools.maven;

import java.io.File;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Compile Scala test source into test-classes.  Corresponds roughly to testCompile
 * in maven-compiler-plugin
 *
 * @phase test-compile
 * @goal testCompile
 * @requiresDependencyResolution test
 */
public class ScalaTestCompileMojo extends ScalaCompilerSupport {

    /**
     * Set this to 'true' to bypass unit tests entirely.
     * Its use is NOT RECOMMENDED, but quite convenient on occasion.
     *
     * @parameter expression="${maven.test.skip}"
     */
    protected boolean skip;

    /**
     * @parameter expression="${project.build.testOutputDirectory}
     */
    protected File testOutputDir;

    /**
     * @parameter expression="${project.build.testSourceDirectory}/../scala"
     */
    protected File testSourceDir;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            return;
        }
        super.execute();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<String> getClasspathElements() throws Exception {
        return project.getTestClasspathElements();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<Dependency> getDependencies() {
        return project.getTestDependencies();
    }

    @Override
    protected File getOutputDir() throws Exception {
        return testOutputDir.getAbsoluteFile();
    }

    
    

    protected List<String> getSourceDirectories() throws Exception {
    	List<String> sources = project.getTestCompileSourceRoots();
    	String scalaSourceDir = testSourceDir.getAbsolutePath();
		if(!sources.contains(scalaSourceDir)) {
    		sources.add(scalaSourceDir);
    	}
    	return sources;
    }
}

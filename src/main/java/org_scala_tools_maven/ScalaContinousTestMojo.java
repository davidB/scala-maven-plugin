/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org_scala_tools_maven;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.maven.BuildFailureException;
import org.apache.maven.shared.invoker.CommandLineConfigurationException;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenCommandLineBuilder;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.invoker.SystemOutHandler;

/**
 * Compile the main and test scala source directory then run unit test cases in continuous (infinite loop).
 * This is an util goal for commandline usage only (Do not use or call it in a pom) !!!
 *
 * @version $Revision: 1.1 $
 * @goal cctest
 * @requiresDependencyResolution test
 */
public class ScalaContinousTestMojo extends ScalaContinuousCompileMojo {

    /**
     * @component
     */
    protected Invoker invoker;


    /**
     * The local repository for caching artifacts. It is strongly recommended to specify a path to an isolated
     * repository like <code>${project.build.directory}/it-repo</code>. Otherwise, your ordinary local repository will
     * be used, potentially soiling it with broken artifacts.
     *
     * @parameter expression="${invoker.localRepositoryPath}" default-value="${settings.localRepository}"
     */
    protected File localRepositoryPath;

    /**
     * Specify this parameter to run individual tests by file name, overriding the <code>includes/excludes</code>
     * parameters.  Each pattern you specify here will be used to create an
     * include pattern formatted like <code>**&#47;${test}.java</code>, so you can just type "-Dtest=MyTest"
     * to run a single test called "foo/MyTest.java".  This parameter will override the TestNG suiteXmlFiles
     * parameter.
     *
     * @parameter expression="${test}"
     */
    protected String test;

    @Override
    protected void postCompileActions() throws Exception {
        if (test == null) {
            getLog().info("Now running all the unit tests. Use -Dtest=FooTest to run a single test by name");
        }
        else {
            getLog().info("Now running tests matching: " + test);
        }

        final InvocationRequest request = new DefaultInvocationRequest();
        request.setLocalRepositoryDirectory(localRepositoryPath);
        request.setInteractive(false);
        request.setErrorHandler(new SystemOutHandler(true));
        request.setOutputHandler(new SystemOutHandler(true));
        request.setBaseDirectory(project.getBasedir());
        request.setPomFile(new File(project.getBasedir(), "pom.xml"));

        request.setGoals(getMavenGoals());
        request.setOffline(false);

        if (test != null) {
            Properties properties = new Properties();
            properties.put("test", test);
            request.setProperties(properties);
        }


        if (getLog().isDebugEnabled()) {
            try {
                getLog().debug("Executing: " + new MavenCommandLineBuilder().build(request));
            }
            catch (CommandLineConfigurationException e) {
                getLog().debug("Failed to display command line: " + e.getMessage());
            }
        }

        try {
            invoker.execute(request);
        }
        catch (final MavenInvocationException e) {
            getLog().debug("Error invoking Maven: " + e.getMessage(), e);
            throw new BuildFailureException("Maven invocation failed. " + e.getMessage(), e);
        }
    }

    protected List<String> getMavenGoals() {
        return Arrays.asList(new String[]{"surefire:test"});
    }
}

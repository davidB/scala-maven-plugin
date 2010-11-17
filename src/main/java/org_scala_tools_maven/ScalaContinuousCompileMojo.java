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

import org.codehaus.plexus.util.FileUtils;
import org_scala_tools_maven_executions.JavaMainCaller;

/**
 * Compile the main and test scala source directory in continuous (infinite loop). !! This is an util goal for commandline usage only (Do not use or call it in a pom) !!!
 *
 * @goal cc
 * @requiresDependencyResolution test
 */
public class ScalaContinuousCompileMojo extends ScalaCompilerSupport {

    /**
     * The output directory for compilation.
     *
     * @parameter expression="${project.build.outputDirectory}"
     */
    protected File mainOutputDir;

    /**
     * The main directory containing scala source for compilation
     *
     * @parameter expression="${project.build.sourceDirectory}/../scala"
     */
    protected File mainSourceDir;

    /**
     * The directory to place test compilation output in
     *
     * @parameter expression="${project.build.testOutputDirectory}
     */
    protected File testOutputDir;

    /**
     * The directory containing test source for compilation
     *
     * @parameter expression="${project.build.testSourceDirectory}/../scala"
     */
    protected File testSourceDir;

    /**
     * Define if fsc should be used, else scalac is used.
     * fsc => scala.tools.nsc.CompileClient, scalac => scala.tools.nsc.Main.
     *
     * @parameter expression="${fsc}" default-value="true"
     */
    protected boolean useFsc = true;

    /**
     * Define if cc should run once or in infinite loop. (useful for test or working with editor)
     * @parameter expression="${once}" default-value="false"
     */
    protected boolean once = false;

    /**
     * Turns verbose output on.
     *
     * @parameter expression="${verbose}" default-value="false"
     */
    protected boolean verbose = false;

    @Override
    protected List<String> getClasspathElements() throws Exception {
        throw new UnsupportedOperationException("USELESS");
    }

    @Override
    protected File getOutputDir() throws Exception {
        throw new UnsupportedOperationException("USELESS");
    }

    @Override
    protected List<File> getSourceDirectories() throws Exception {
           throw new UnsupportedOperationException("USELESS");
    }
    @Override
    protected JavaMainCaller getScalaCommand() throws Exception {
        JavaMainCaller jcmd = super.getScalaCommand();
        if (useFsc && verbose) {
            jcmd.addOption("-verbose", verbose);
        }
        return jcmd;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected final void doExecute() throws Exception {

        mainOutputDir = normalize(mainOutputDir);
        if (!mainOutputDir.exists()) {
            mainOutputDir.mkdirs();
        }
        mainSourceDir = normalize(mainSourceDir);

        testOutputDir = normalize(testOutputDir);
        if (!testOutputDir.exists()) {
            testOutputDir.mkdirs();
        }
        testSourceDir = normalize(testSourceDir);

        if (useFsc) {
            getLog().info("use fsc for compilation");
            scalaClassName = "scala.tools.nsc.CompileClient";
            if (!once) {
                StopServer stopServer = new StopServer();
                stopServer.run();
                startNewCompileServer();
                Runtime.getRuntime().addShutdownHook(stopServer);
            } else {
                startNewCompileServer();
            }
        }

        getLog().info("wait for files to compile...");
        do {
            clearCompileErrors();

            int nbFile = 0;
            if (mainSourceDir.exists()) {
                nbFile = compile(mainSourceDir, mainOutputDir, project.getCompileClasspathElements(), true);
                // If there are no source files, the compile method returns -1. Thus, to make sure we
                // still run the tests if there are test sources, reset nbFile to zero.
                if (nbFile == -1)
                    nbFile = 0;
            }
            if (testSourceDir.exists()) {
                nbFile += compile(testSourceDir, testOutputDir, project.getTestClasspathElements(), true);
            }
            if (nbFile > 0) {
                if (!hasCompileErrors()) {
                    postCompileActions();
                } else {
                    getLog().info("Not running test cases due to compile error");
                }
            }
            if (!once) {
                if (nbFile > 0) {
                    getLog().info("wait for files to compile...");
                    Thread.sleep(5000);
                } else {
                    Thread.sleep(3000);
                }
            }
        } while (!once);
    }



    /**
     * Allows derived Mojos to do things after a compile has succesfully completed such as run test cases
     */
    protected void postCompileActions() throws Exception {
    }

    private void startNewCompileServer() throws Exception {
        File serverTagFile = new File(mainOutputDir + ".server");
        if (serverTagFile.exists()) {
            return;
        }
        getLog().info("start server...");
        JavaMainCaller jcmd = getEmptyScalaCommand("scala.tools.nsc.MainGenericRunner");
        jcmd.addArgs("scala.tools.nsc.CompileServer");
        jcmd.addJvmArgs(jvmArgs);
        jcmd.addArgs(args);
        jcmd.spawn(displayCmd);
        FileUtils.fileWrite(serverTagFile.getAbsolutePath(), ".");
        Thread.sleep(1000); //HACK To wait startup time of server (avoid first fsc command to failed to contact server)
    }

    private class StopServer extends Thread {
        @Override
        public void run() {
            try {
                getLog().info("stop server(s)...");
                JavaMainCaller jcmd = getScalaCommand();
                jcmd.addArgs("-shutdown");
                jcmd.run(displayCmd, false);
                File serverTagFile = new File(mainOutputDir + ".server");
                if (serverTagFile.exists()) {
                    serverTagFile.delete();
                }
            } catch (Exception exc) {
                //getLog().warn(exc);
            }
        }
    }

}

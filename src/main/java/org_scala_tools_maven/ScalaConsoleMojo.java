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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org_scala_tools_maven_executions.JavaMainCaller;
import org_scala_tools_maven_executions.JavaMainCallerInProcess;
import org_scala_tools_maven_executions.MainHelper;

/**
 * Run the Scala console with all the classes of the projects (dependencies and builded)
 *
 * @goal console
 * @requiresDependencyResolution test
 * @inheritByDefault false
 * @requiresDirectInvocation true
 * @executionStrategy once-per-session
 */
public class ScalaConsoleMojo extends ScalaMojoSupport {

    /**
     * The console to run.
     *
     * @parameter expression="${mainConsole}" default-value="scala.tools.nsc.MainGenericRunner"
     * @required
     */
    protected String mainConsole;

    /**
     * Add the test classpath (include classes from test directory), to the console's classpath ?
     *
     * @parameter expression="${maven.scala.console.useTestClasspath}" default-value="true"
     * @required
     */
    protected boolean useTestClasspath;

    /**
     * Add the runtime classpath, to the console's classpath ?
     *
     * @parameter expression="${maven.scala.console.useRuntimeClasspath}" default-value="true"
     * @required
     */
    protected boolean useRuntimeClasspath;

    /**
     * Path of the javaRebel jar. If this option is set then the console run
     * with <a href="http://www.zeroturnaround.com/javarebel/">javarebel</a> enabled.
     *
     * @parameter expression="${javarebel.jar.path}"
     */
    protected File javaRebelPath;

    @Override
    @SuppressWarnings("unchecked")
    protected void doExecute() throws Exception {
        //TODO - Many other paths uses the getScalaCommand()!!! We should try to use that as much as possibel to help maintainability.
        String sv = findScalaVersion().toString();
        Set<String> classpath = new LinkedHashSet<String>();
        addCompilerToClasspath(sv, classpath);
        addLibraryToClasspath(sv, classpath);
        addToClasspath("jline", "jline", "0.9.94", classpath);
        classpath.addAll(project.getCompileClasspathElements());
        if (useTestClasspath) {
            classpath.addAll(project.getTestClasspathElements());
        }
        if (useRuntimeClasspath) {
            classpath.addAll(project.getRuntimeClasspathElements());
        }
        String classpathStr = MainHelper.toMultiPath(classpath.toArray(new String[classpath.size()]));
        JavaMainCaller jcmd = null;
        List<String> list = new ArrayList<String>(args != null ? args.length + 3 : 3);
        if(args != null) {
            for(String arg : args) {
                list.add(arg);
            }
        }
        list.add("-cp");
        list.add(classpathStr);

        if(fork) {
            getLog().warn("maven-scala-plugin cannot fork scala console!!  Running in process");
        }

        jcmd = new JavaMainCallerInProcess(this, mainConsole, classpathStr, jvmArgs, list.toArray(new String[list.size()]));
        //We need to make sure compiler plugins are sent into the interpreter as well!
        addCompilerPluginOptions(jcmd);
        if (javaRebelPath != null) {
            if (!javaRebelPath.exists()) {
                getLog().warn("javaRevelPath '"+javaRebelPath.getCanonicalPath()+"' not found");
            } else {
                jcmd.addJvmArgs("-noverify", "-javaagent:" + javaRebelPath.getCanonicalPath());
            }
        }
        jcmd.run(displayCmd);
    }
}

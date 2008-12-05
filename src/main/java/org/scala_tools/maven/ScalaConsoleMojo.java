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
import java.util.HashSet;
import java.util.Set;

/**
 * Run the Scala console with all the classes of the projects (dependencies and builded)
 *
 * @goal console
 * @execute phase="compile"
 * @requiresDependencyResolution test
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
        Set<String> classpath = new HashSet<String>();
        addToClasspath("org.scala-lang", "scala-compiler", scalaVersion, classpath);
        addToClasspath("org.scala-lang", "scala-library", scalaVersion, classpath);
        classpath.addAll(project.getCompileClasspathElements());
        if (useTestClasspath) {
            classpath.addAll(project.getTestClasspathElements());
        }
        if (useRuntimeClasspath) {
            classpath.addAll(project.getRuntimeClasspathElements());
        }
        String classpathStr = JavaCommand.toMultiPath(classpath.toArray(new String[classpath.size()]));
        JavaCommand jcmd = new JavaCommand(this, mainConsole, classpathStr, jvmArgs, args);
        if (javaRebelPath != null) {
            if (!javaRebelPath.exists()) {
                getLog().warn("javaRevelPath '"+javaRebelPath.getCanonicalPath()+"' not found");
            } else {
                jcmd.addJvmArgs("-noverify", "-javaagent:" + javaRebelPath.getCanonicalPath());
            }
        }
        jcmd.setLogOnly(false);
        jcmd.run(displayCmd);
    }
}

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

import org.codehaus.plexus.util.StringUtils;


/**
 * Run a Scala class using the Scala runtime
 *
 * @goal run
 * @requiresDependencyResolution test
 * @execute phase="test-compile"
 */
public class ScalaRunMojo extends ScalaMojoSupport {

    /**
     * @parameter expression="${launcher}"
     */
    protected String launcher;

    /**
     * Additional parameter to use to call the main class
     * Using this parameter only from command line ("-DaddArgs=arg1|arg2|arg3|..."), not from pom.xml.
     * @parameter expression="${addArgs}"
     */
    protected String addArgs;

    /**
     * A list of launcher definition (to avoid rewriting long command line or share way to call an application)
     * launchers could be define by :
     * <pre>
     *   &lt;launchers>
     *     &lt;launcher>
     *       &lt;id>myLauncher&lt;/id>
     *       &lt;mainClass>my.project.Main&lt;/mainClass>
     *       &lt;args>
     *         &lt;arg>arg1&lt;/arg>
     *       &lt;/args>
     *       &lt;jvmArgs>
     *         &lt;jvmArg>-Xmx64m&lt;/jvmArg>
     *       &lt;/jvmArgs>
     *     &lt;/launcher>
     *     &lt;launcher>
     *       &lt;id>myLauncher2&lt;/id>
     *       ...
     *       &lt;>&lt;>
     *     &lt;/launcher>
     *   &lt;/launchers>
     * </pre>
     * @parameter
     */
    protected Launcher[] launchers;

    /**
     * Main class to call, the call use the jvmArgs and args define in the pom.xml, and the addArgs define in the command line if define.
     *
     * Higher priority to launcher parameter)
     * Using this parameter only from command line (-DmainClass=...), not from pom.xml.
     * @parameter expression="${mainClass}"
     */
    protected String mainClass;

    @Override
    @SuppressWarnings("unchecked")
    protected void doExecute() throws Exception {
        JavaCommand jcmd = null;
        if (StringUtils.isNotEmpty(mainClass)) {
            jcmd = new JavaCommand(this, mainClass, JavaCommand.toMultiPath(project.getTestClasspathElements()), jvmArgs, args);
        } else if ((launchers != null) && (launchers.length > 0)) {
            if (StringUtils.isNotEmpty(launcher)) {
                for(int i = 0; (i < launchers.length) && (jcmd == null); i++) {
                    if (launcher.equals(launchers[i].id)) {
                        getLog().info("launcher '"+ launchers[i].id + "' selected => "+ launchers[i].mainClass );
                        jcmd = new JavaCommand(this, launchers[i].mainClass, JavaCommand.toMultiPath(project.getTestClasspathElements()), launchers[i].jvmArgs, launchers[i].args);
                    }
                }
            } else {
                getLog().info("launcher '"+ launchers[0].id + "' selected => "+ launchers[0].mainClass );
                jcmd = new JavaCommand(this, launchers[0].mainClass, JavaCommand.toMultiPath(project.getTestClasspathElements()), launchers[0].jvmArgs, launchers[0].args);
            }
        }
        if (jcmd != null) {
            if (StringUtils.isNotEmpty(addArgs)) {
                jcmd.addArgs(addArgs.split("|"));
            }
            jcmd.run(displayCmd);
        } else {
            getLog().warn("Not mainClass or valid launcher found/define");
        }
    }
}

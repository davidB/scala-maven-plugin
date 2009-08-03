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

import org.scala_tools.maven.executions.JavaMainCaller;


/**
 * Display the Scala Compiler help
 *
 * @goal help
 */
public class ScalaHelpMojo extends ScalaMojoSupport {
    /**
     * Determines if help will only display a version
     * @parameter expression="${maven.scala.help.versionOnly}" default-value="false"
     */
    private boolean versionOnly;

    @Override
    public void doExecute() throws Exception {
        JavaMainCaller jcmd = null;
        if (!versionOnly) {
            jcmd = getScalaCommand();
            jcmd.addArgs("-help");
            jcmd.addArgs("-X");
            jcmd.addArgs("-Y");
            jcmd.run(displayCmd);
        }
        jcmd = getScalaCommand();
        jcmd.addArgs("-version");
        jcmd.run(displayCmd);
    }
}

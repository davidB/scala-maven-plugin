
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

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import scala_maven_executions.JavaMainCaller;

/** Display the Scala Compiler help */
@Mojo(name = "help")
public class ScalaHelpMojo extends ScalaMojoSupport {
  /** Determines if help will only display a version */
  @Parameter(property = "maven.scala.help.versionOnly", defaultValue = "false")
  private boolean versionOnly;

  @Override
  public void doExecute() throws Exception {
    JavaMainCaller jcmd;
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

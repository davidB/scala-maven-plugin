
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
package util;

import java.io.File;
import org.apache.maven.toolchain.Toolchain;

/**
 * Utilities to aid with finding Java's location
 *
 * @author C. Dessonville
 */
public class JavaLocator {

  public static String findExecutableFromToolchain(Toolchain toolchain) {
    String javaExec = null;

    if (toolchain != null) {
      javaExec = toolchain.findTool("java");
    }

    if (javaExec == null) {
      String javaHome = System.getenv("JAVA_HOME");
      if (javaHome == null) {
        javaHome = System.getProperty("java.home"); // fallback to JRE
      }
      if (javaHome == null) {
        throw new IllegalStateException(
            "Couldn't locate java, try setting JAVA_HOME environment variable.");
      }
      javaExec = javaHome + File.separator + "bin" + File.separator + "java";
    }

    return javaExec;
  }

  public static File findHomeFromToolchain(Toolchain toolchain) {
    String executable = findExecutableFromToolchain(toolchain);
    File executableParent = new File(executable).getParentFile();
    if (executableParent == null) {
      return null;
    }
    return executableParent.getParentFile();
  }
}

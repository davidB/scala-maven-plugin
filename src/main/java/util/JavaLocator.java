/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
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

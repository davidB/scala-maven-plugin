/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package util;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import org.apache.maven.toolchain.Toolchain;

/**
 * Utilities to aid with finding Java's location
 *
 * @author C. Dessonville
 */
public class JavaLocator {

  private static final boolean IS_WINDOWS =
      System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("windows");

  // inspired from org.codehaus.plexus.compiler.javac.JavacCompiler#getJavacExecutable
  public static String findExecutableFromToolchain(Toolchain toolchain) {

    if (toolchain != null) {
      String fromToolChain = toolchain.findTool("java");
      if (fromToolChain != null) {
        return fromToolChain;
      }
    }

    String javaCommand = "java" + (IS_WINDOWS ? ".exe" : "");

    String javaHomeSystemProperty = System.getProperty("java.home");
    if (javaHomeSystemProperty != null) {
      Path javaHomePath = Paths.get(javaHomeSystemProperty);

      if (javaHomePath.endsWith("jre")) {
        // Old JDK versions contain a JRE. We might be pointing to that.
        // We want to try to use the JDK instead as we need javac in order to compile mixed
        // Java-Scala projects.
        Path javaExecPath = javaHomePath.resolveSibling("bin").resolve(javaCommand);
        if (javaExecPath.toFile().isFile()) {
          return javaExecPath.toString();
        }
      }

      // old standalone JRE or modern JDK
      Path javaExecPath = javaHomePath.resolve("bin").resolve(javaCommand);
      if (javaExecPath.toFile().isFile()) {
        return javaExecPath.toString();
      } else {
        throw new IllegalStateException(
            "Couldn't locate java in defined java.home system property.");
      }
    }

    // fallback: try to resolve from JAVA_HOME
    String javaHomeEnvVar = System.getenv("JAVA_HOME");
    if (javaHomeEnvVar == null) {
      throw new IllegalStateException(
          "Couldn't locate java, try setting JAVA_HOME environment variable.");
    }

    Path javaExecPath = Paths.get(javaHomeEnvVar).resolve("bin").resolve(javaCommand);
    if (javaExecPath.toFile().isFile()) {
      return javaExecPath.toString();
    } else {
      throw new IllegalStateException(
          "Couldn't locate java in defined JAVA_HOME environment variable.");
    }
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

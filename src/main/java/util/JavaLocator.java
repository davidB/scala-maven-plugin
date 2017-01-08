package util;

import org.apache.maven.toolchain.Toolchain;

import java.io.File;

/**
 * Utilities to aid with finding Java's location
 *
 * @Author C. Dessonville
 */
public class JavaLocator {

  public static String findExecutableFromToolchain(Toolchain toolchain) {
    String _javaExec = null;

    if (toolchain != null)
      _javaExec = toolchain.findTool("java");

    if (toolchain == null || _javaExec == null) {
      _javaExec = System.getProperty("java.home");
      if (_javaExec == null) {
        _javaExec = System.getenv("JAVA_HOME");
        if (_javaExec == null) {
          throw new IllegalStateException("Couldn't locate java, try setting JAVA_HOME environment variable.");
        }
      }

      _javaExec += File.separator + "bin" + File.separator + "java";
    }

    return _javaExec;
  }

  public static String findHomeFromToolchain(Toolchain toolchain) {
    String executable = findExecutableFromToolchain(toolchain);
    if (executable != null) {
      File executableParent = new File(executable).getParentFile();
      if (executableParent != null) {
        return executableParent.getParent();
      } else {
        return null;
      }
    } else {
      return null;
    }
  }
}

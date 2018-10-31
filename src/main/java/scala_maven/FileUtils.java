package scala_maven;

import java.io.File;

class FileUtils extends org.codehaus.plexus.util.FileUtils {

  /**
   * @param canonical Should use CanonicalPath to normalize path (true => getCanonicalPath, false =&gt; getAbsolutePath)
   * @see <a href="https://github.com/davidB/maven-scala-plugin/issues/50">#50</a>
   */
  public static String pathOf(File f, boolean canonical) throws Exception {
    return canonical? f.getCanonicalPath() : f.getAbsolutePath();
  }

  /**
   * @param canonical Should use CanonicalPath to normalize path (true => getCanonicalPath, false =&gt; getAbsolutePath)
   * @see <a href="https://github.com/davidB/maven-scala-plugin/issues/50">#50</a>
   */
  public static File fileOf(File f, boolean canonical) throws Exception {
    return canonical? f.getCanonicalFile() : f.getAbsoluteFile();
  }
}

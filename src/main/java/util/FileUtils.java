/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FileUtils {

  /**
   * @param canonical Should use CanonicalPath to normalize path (true => getCanonicalPath, false
   *     =&gt; getAbsolutePath)
   * @see <a href="https://github.com/davidB/maven-scala-plugin/issues/50">#50</a>
   */
  public static String pathOf(File f, boolean canonical) throws Exception {
    return canonical ? f.getCanonicalPath() : f.getAbsolutePath();
  }

  /**
   * @param canonical Should use CanonicalPath to normalize path (true => getCanonicalPath, false
   *     =&gt; getAbsolutePath)
   * @see <a href="https://github.com/davidB/maven-scala-plugin/issues/50">#50</a>
   */
  public static File fileOf(File f, boolean canonical) throws Exception {
    return canonical ? f.getCanonicalFile() : f.getAbsoluteFile();
  }

  public static Set<File> fromStrings(Collection<String> s) {
    return s.stream().map(File::new).collect(Collectors.toSet());
  }

  public static String toMultiPath(Collection<File> paths) {
    return paths.stream().map(File::getPath).collect(Collectors.joining(File.pathSeparator));
  }

  public static List<Path> listDirectoryContent(Path directory, Function<Path, Boolean> filter)
      throws IOException {
    List<Path> paths = new ArrayList<>();
    Files.walkFileTree(
        directory,
        new SimpleFileVisitor<Path>() {
          @Override
          public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
            if (filter.apply(path)) {
              paths.add(path);
            }
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            if (!dir.equals(directory) && filter.apply(dir)) {
              paths.add(dir);
            }
            return FileVisitResult.CONTINUE;
          }
        });
    return paths;
  }

  public static void deleteDirectory(Path directory) {
    try {
      Files.walkFileTree(
          directory,
          new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
              Files.delete(file);
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                throws IOException {
              Files.delete(dir);
              return FileVisitResult.CONTINUE;
            }
          });
    } catch (Exception e) {
      // life...
    }
  }
}


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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.codehaus.plexus.util.StringUtils;

public class FileUtils extends org.codehaus.plexus.util.FileUtils {

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
    return StringUtils.join(paths.iterator(), File.pathSeparator);
  }

  public static String toMultiPath(File[] paths) {
    return StringUtils.join(paths, File.pathSeparator);
  }

  public static URL[] toUrls(File[] files) throws Exception {
    return Stream.of(files)
        .map(
            x -> {
              try {
                return x.toURI().toURL();
              } catch (MalformedURLException e) {
                throw new RuntimeException("failed to convert into url " + x, e);
              }
            })
        .toArray(URL[]::new);
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

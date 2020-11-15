
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

import java.io.File;

class FileUtils extends org.codehaus.plexus.util.FileUtils {

  /**
   * @param canonical Should use CanonicalPath to normalize path (true => getCanonicalPath, false
   *     =&gt; getAbsolutePath)
   * @see <a href="https://github.com/davidB/maven-scala-plugin/issues/50">#50</a>
   */
  static String pathOf(File f, boolean canonical) throws Exception {
    return canonical ? f.getCanonicalPath() : f.getAbsolutePath();
  }

  /**
   * @param canonical Should use CanonicalPath to normalize path (true => getCanonicalPath, false
   *     =&gt; getAbsolutePath)
   * @see <a href="https://github.com/davidB/maven-scala-plugin/issues/50">#50</a>
   */
  static File fileOf(File f, boolean canonical) throws Exception {
    return canonical ? f.getCanonicalFile() : f.getAbsoluteFile();
  }
}

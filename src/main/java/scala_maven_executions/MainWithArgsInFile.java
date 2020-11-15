
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
package scala_maven_executions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Use a file and reflection to start a main class with arguments define in a file. This class
 * should run without other dependencies than jre. This class is used as a workaround to the windows
 * command line size limitation.
 *
 * @author David Bernard
 */
public class MainWithArgsInFile {

  /** @param args */
  public static void main(String[] args) {
    try {
      String mainClassName = args[0];
      List<String> argsFromFile = new ArrayList<>();
      if (args.length > 0) {
        argsFromFile = MainHelper.readArgFile(new File(args[1]));
      }
      MainHelper.runMain(mainClassName, argsFromFile, null);
    } catch (Throwable t) {
      t.printStackTrace();
      System.exit(-10000 /* Integer.MIN_VALUE */);
    }
  }
}

/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package scala_maven_executions;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class ForkMain {

  public static final String ARGS_FILE = "META-INF/args.txt";

  public static void main(String[] args) {
    try {
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      String[] argsFromFile = readArgFile(cl);
      runMain(cl, args[0], argsFromFile);
    } catch (Throwable t) {
      PrintWriter stacktrace = new PrintWriter(new StringWriter());
      t.printStackTrace(stacktrace);
      System.out.println(ForkLogLevel.ERROR.addHeader(stacktrace.toString()));
      System.out.flush();
      System.exit(-1);
    }
  }

  private static void runMain(ClassLoader cl, String mainClassName, String[] args)
      throws Exception {
    Class<?> mainClass = cl.loadClass(mainClassName);
    Method mainMethod = mainClass.getMethod("main", String[].class);
    int mods = mainMethod.getModifiers();
    if (mainMethod.getReturnType() != void.class
        || !Modifier.isStatic(mods)
        || !Modifier.isPublic(mods)) {
      throw new NoSuchMethodException("main");
    }

    mainMethod.invoke(null, new Object[] {args});
  }

  private static String[] readArgFile(ClassLoader cl) throws IOException {
    List<String> args = new ArrayList<>();
    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(cl.getResourceAsStream(ARGS_FILE), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        args.add(line);
      }
      return args.toArray(new String[] {});
    }
  }
}

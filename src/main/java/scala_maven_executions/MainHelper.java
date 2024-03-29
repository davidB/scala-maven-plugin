/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package scala_maven_executions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.codehaus.plexus.util.DirectoryScanner;

/**
 * Helper methods
 *
 * @author David Bernard
 */
public class MainHelper {

  static final String argFilePrefix = "scala-maven-";
  static final String argFileSuffix = ".args";

  public static String[] findFiles(File dir, String[] includes, String[] excludes) {
    DirectoryScanner scanner = new DirectoryScanner();
    scanner.setBasedir(dir);
    scanner.setIncludes(includes);
    scanner.setExcludes(excludes);
    scanner.addDefaultExcludes();
    scanner.scan();
    return scanner.getIncludedFiles();
  }

  public static String toClasspathString(ClassLoader cl) {
    StringBuilder back = new StringBuilder();
    List<String> cps = new ArrayList<>();
    appendUrlToClasspathCollection(cl, cps);
    for (String cp : cps) {
      if (back.length() != 0) {
        back.append(File.pathSeparatorChar);
      }
      back.append(cp);
    }
    return back.toString();
  }

  private static void appendUrlToClasspathCollection(ClassLoader cl, Collection<String> classpath) {
    if (cl == null) {
      cl = Thread.currentThread().getContextClassLoader();
    }
    while (cl != null) {
      if (cl instanceof URLClassLoader) {
        URLClassLoader ucl = (URLClassLoader) cl;
        URL[] urls = ucl.getURLs();
        for (URL url : urls) {
          classpath.add(url.getFile());
        }
      }
      cl = cl.getParent();
    }
  }

  /**
   * Escapes arguments as necessary so the StringTokenizer for scala arguments pulls in filenames
   * with spaces correctly.
   *
   * @param arg
   * @return
   */
  private static String escapeArgumentForScalacArgumentFile(String arg) {
    if (arg.matches(".*\\s.*")) {
      return '"' + arg + '"';
    }
    return arg;
  }

  /**
   * UnEscapes arguments as necessary so the StringTokenizer for scala arguments pulls in filenames
   * with spaces correctly.
   *
   * @param arg
   * @return
   */
  private static String unescapeArgumentForScalacArgumentFile(String arg) {
    if (arg.charAt(0) == '"' && arg.charAt(arg.length() - 1) == '"') {
      return arg.substring(1, arg.length() - 1);
    }
    return arg;
  }

  /**
   * Creates a file containing all the arguments. This file has a very simple format of argument
   * (white-space argument).
   *
   * @return
   * @throws IOException
   */
  static File createArgFile(List<String> args) throws IOException {
    final File argFile = File.createTempFile(argFilePrefix, argFileSuffix);
    // argFile.deleteOnExit();
    try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(argFile)))) {
      for (String arg : args) {
        out.println(escapeArgumentForScalacArgumentFile(arg));
      }
    }
    return argFile;
  }

  /**
   * Creates a file containing all the arguments. This file has a very simple format of argument
   * (white-space argument).
   *
   * @return
   * @throws IOException
   */
  static List<String> readArgFile(File argFile) throws IOException {
    ArrayList<String> back = new ArrayList<>();
    try (BufferedReader in = new BufferedReader(new FileReader(argFile))) {
      String line;
      while ((line = in.readLine()) != null) {
        back.add(unescapeArgumentForScalacArgumentFile(line));
      }
    }
    return back;
  }

  /** Runs the main method of a java class */
  static void runMain(String mainClassName, List<String> args, ClassLoader cl) throws Exception {
    if (cl == null) {
      cl = Thread.currentThread().getContextClassLoader();
    }
    Class<?> mainClass = cl.loadClass(mainClassName);
    Method mainMethod = mainClass.getMethod("main", String[].class);
    int mods = mainMethod.getModifiers();
    if (mainMethod.getReturnType() != void.class
        || !Modifier.isStatic(mods)
        || !Modifier.isPublic(mods)) {
      throw new NoSuchMethodException("main");
    }
    String[] argArray = args.toArray(new String[] {});

    // TODO - Redirect System.in System.err and System.out

    mainMethod.invoke(null, new Object[] {argArray});
  }

  static String locateJar(Class<?> c) throws Exception {
    final URL location;
    final String classLocation = c.getName().replace('.', '/') + ".class";
    final ClassLoader loader = c.getClassLoader();
    if (loader == null) {
      location = ClassLoader.getSystemResource(classLocation);
    } else {
      location = loader.getResource(classLocation);
    }
    if (location != null) {
      Pattern p = Pattern.compile("^.*file:(.*)!.*$");
      Matcher m = p.matcher(location.toString());
      if (m.find()) {
        return URLDecoder.decode(m.group(1), "UTF-8");
      }
      throw new ClassNotFoundException(
          "Cannot parse location of '" + location + "'.  Probably not loaded from a Jar");
    }
    throw new ClassNotFoundException(
        "Cannot find class '" + c.getName() + " using the classloader");
  }
}

/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package scala_maven_executions;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import org.apache.commons.exec.*;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Run a forked Java process, based on a generated booter jar. The classpath is passed as a manifest
 * entry to cope with command length limitation on Windows. The target main class is passed as an
 * argument. The target arguments are passed as a file inside the jar.
 */
public final class Fork {

  private static final boolean IS_WINDOWS =
      System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("windows");

  private static final String BOOTER_JAR_NAME = "scala-maven-plugin-booter";
  private final File javaExecutable;
  private final String mainClassName;
  private final List<String> classpath;

  private final String[] jvmArgs;
  private final String[] args;

  public Fork(
      String mainClassName,
      List<String> classpath,
      String[] jvmArgs,
      String[] args,
      File javaExecutable) {

    this.mainClassName = mainClassName;
    this.classpath = classpath;
    this.jvmArgs = jvmArgs;
    this.args = args;
    this.javaExecutable = javaExecutable;
  }

  private static String toWindowsShortName(String value) {
    if (IS_WINDOWS) {
      int programFilesIndex = value.indexOf("Program Files");
      if (programFilesIndex >= 0) {
        // Could be "Program Files" or "Program Files (x86)"
        int firstSeparatorAfterProgramFiles =
            value.indexOf(File.separator, programFilesIndex + "Program Files".length());
        File longNameDir =
            firstSeparatorAfterProgramFiles < 0
                ? new File(value)
                : // C:\\Program Files with
                // trailing separator
                new File(value.substring(0, firstSeparatorAfterProgramFiles)); // chop child
        // Some other sibling dir could be PrograXXX and might shift short name index
        // so, we can't be sure "Program Files" is "Progra~1" and "Program Files (x86)"
        // is "Progra~2"
        for (int i = 0; i < 10; i++) {
          File shortNameDir = new File(longNameDir.getParent(), "Progra~" + i);
          if (shortNameDir.equals(longNameDir)) {
            return shortNameDir.toString();
          }
        }
      }
    }

    return value;
  }

  public void run(OutputStream os) throws Exception {
    File booterJar = createBooterJar(classpath, ForkMain.class.getName(), args);

    CommandLine command = new CommandLine(toWindowsShortName(javaExecutable.getCanonicalPath()));
    command.addArguments(jvmArgs, false);
    command.addArgument("-jar");
    command.addArgument(booterJar.getCanonicalPath());
    command.addArgument(mainClassName);

    Executor exec = new DefaultExecutor();
    exec.setStreamHandler(new PumpStreamHandler(os));

    int exitValue = exec.execute(command);
    if (exitValue != 0) {
      throw new MojoFailureException("command line returned non-zero value:" + exitValue);
    }
  }

  /**
   * Create a jar with just a manifest containing a Main-Class entry and a Class-Path entry for all
   * classpath elements.
   *
   * @param classPath List of all classpath elements.
   * @param startClassName The classname to start (main-class)
   * @return The file pointing to the jar
   * @throws IOException When a file operation fails.
   */
  private static File createBooterJar(List<String> classPath, String startClassName, String[] args)
      throws IOException {
    File file = File.createTempFile(BOOTER_JAR_NAME, ".jar");
    file.deleteOnExit();

    String cp =
        classPath.stream()
            .map(element -> getURL(new File(element)).toExternalForm())
            .collect(Collectors.joining(" "));

    Manifest manifest = new Manifest();
    manifest.getMainAttributes().putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
    manifest.getMainAttributes().putValue(Attributes.Name.MAIN_CLASS.toString(), startClassName);
    manifest.getMainAttributes().putValue(Attributes.Name.CLASS_PATH.toString(), cp);

    try (JarOutputStream jos =
        new JarOutputStream(new BufferedOutputStream(Files.newOutputStream(file.toPath())))) {
      jos.setLevel(JarOutputStream.STORED);

      JarEntry manifestJarEntry = new JarEntry("META-INF/MANIFEST.MF");
      jos.putNextEntry(manifestJarEntry);
      manifest.write(jos);
      jos.closeEntry();

      JarEntry argsJarEntry = new JarEntry(ForkMain.ARGS_FILE);
      jos.putNextEntry(argsJarEntry);
      jos.write(
          Arrays.stream(args).collect(Collectors.joining("\n")).getBytes(StandardCharsets.UTF_8));
      jos.closeEntry();
    }

    return file;
  }

  // encode any characters that do not comply with RFC 2396
  // this is primarily to handle Windows where the user's home directory contains
  // spaces
  private static URL getURL(File file) {
    try {
      return new URL(file.toURI().toASCIIString());
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }
}

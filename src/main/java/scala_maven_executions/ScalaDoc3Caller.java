/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package scala_maven_executions;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;

/**
 * @author Chr. Reichardt
 */
public class ScalaDoc3Caller implements JavaMainCaller {

  private static final Map<String, String> COMPAT_MAP = new HashMap<>();

  static {
    COMPAT_MAP.put("-doc-footer", "-project-footer");
    COMPAT_MAP.put("-doc-title", "-project");
    COMPAT_MAP.put("-doc-version", "-project-version");
    COMPAT_MAP.put("-doc-source-url", "-source-links");
  }

  String classPath;
  final List<String> jvmArgs = new ArrayList<>();
  final String apidocMainClassName;
  final AbstractMojo requester;
  String outputPath =
      FileSystems.getDefault()
          .getPath(".", "target", "site", "scaladocs")
          .toAbsolutePath()
          .toString();
  final List<String> args = new ArrayList<>();
  final String targetClassesDir;

  public ScalaDoc3Caller(AbstractMojo mojo, String apidocMainClassName, String targetClassesDir) {
    this.requester = mojo;
    this.apidocMainClassName = apidocMainClassName;
    this.targetClassesDir = targetClassesDir;
  }

  @Override
  public void addJvmArgs(String... jvmArgs) {
    if (Objects.nonNull(jvmArgs)) {
      this.jvmArgs.addAll(Arrays.asList(jvmArgs));
    }
  }

  @Override
  public void addArgs(String... args) {
    if (Objects.isNull(args) || args.length == 0) {
      return;
    }
    List<String> migratedArgs =
        Arrays.stream(args)
            .filter(arg -> !Objects.equals(arg, "-doc-format:html"))
            .map(arg -> COMPAT_MAP.getOrDefault(arg, arg))
            .collect(Collectors.toList());
    this.args.addAll(migratedArgs);
  }

  @Override
  public void addOption(String key, String value) {
    if (key.equals("-classpath")) {
      this.classPath = value;
    } else if (key.equals("-d")) {
      this.outputPath = value;
    } else {
      this.args.add(COMPAT_MAP.getOrDefault(key, key));
      this.args.add(value);
    }
  }

  @Override
  public void addOption(String key, File value) {
    if (Objects.nonNull(value)) {
      addOption(key, value.getAbsolutePath());
    }
  }

  @Override
  public void addOption(String key, boolean value) {
    if (value) {
      this.args.add(COMPAT_MAP.getOrDefault(key, key));
    }
  }

  @Override
  public void redirectToLog() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void run(boolean displayCmd) throws Exception {
    run(displayCmd, true);
  }

  @Override
  public boolean run(boolean displayCmd, boolean throwFailure) throws MojoFailureException {
    List<String> commands = new ArrayList<>();
    commands.add("java");
    commands.addAll(this.jvmArgs);
    if (Objects.nonNull(this.classPath)) {
      commands.add("-classpath");
      commands.add(this.classPath);
    }
    commands.add("-Dscala.usejavacp=true");
    commands.add(this.apidocMainClassName);
    commands.addAll(this.args);
    commands.add("-d");
    commands.add(this.outputPath);
    commands.add(this.targetClassesDir);

    if (displayCmd) {
      String cmd = commands.stream().collect(Collectors.joining(" "));
      this.requester.getLog().info(String.format("cmd: %s", cmd));
    }

    ProcessBuilder processBuilder = new ProcessBuilder(commands);
    Path userDir = FileSystems.getDefault().getPath(".");
    File workingDir = userDir.toFile();
    Process process;
    try {
      process =
          processBuilder
              .directory(workingDir)
              .redirectOutput(ProcessBuilder.Redirect.INHERIT)
              .redirectError(ProcessBuilder.Redirect.INHERIT)
              .start();
    } catch (IOException ex) {
      throw new MojoFailureException(ex.getMessage(), ex);
    }
    try {
      int exitValue = process.waitFor();
      if (exitValue != 0) {
        throw new MojoFailureException(
            String.format("scaladoc_3 returned non-zero value: %d", exitValue));
      }
    } catch (InterruptedException ex) {
      throw new MojoFailureException(ex.getMessage(), ex);
    }

    return true;
  }

  @Override
  public SpawnMonitor spawn(boolean displayCmd) throws Exception {
    throw new UnsupportedOperationException("Not supported yet.");
  }
}

/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package scala_maven;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import sbt.internal.inc.ScalaInstance;
import sbt_inc.SbtIncrementalCompiler;
import scala.Option;
import scala_maven_dependency.Context;
import scala_maven_executions.JavaMainCaller;
import util.FileUtils;
import util.JavaLocator;
import xsbti.compile.CompileOrder;

/** Abstract parent of all Scala Mojo who run compilation */
public abstract class ScalaCompilerSupport extends ScalaSourceMojoSupport {

  public enum RecompileMode {
    /** all sources are recompiled */
    all,

    /** incrementally recompile modified sources and other affected sources */
    incremental
  }

  /** Keeps track of if we get compile errors in incremental mode */
  private boolean compileErrors;

  /**
   * Recompile mode to use when sources were previously compiled and there is at least one change,
   * see {@link RecompileMode}.
   */
  @Parameter(property = "recompileMode", defaultValue = "incremental")
  RecompileMode recompileMode;

  /**
   * notifyCompilation if true then print a message "path: compiling" for each root directory or
   * files that will be compiled. Useful for debug, and for integration with Editor/IDE to reset
   * markers only for compiled files.
   */
  @Parameter(property = "notifyCompilation", defaultValue = "true")
  private boolean notifyCompilation;

  /**
   * Compile order for Scala and Java sources for sbt incremental compile.
   *
   * <p>Can be Mixed, JavaThenScala, or ScalaThenJava.
   */
  @Parameter(property = "compileOrder", defaultValue = "Mixed")
  private CompileOrder compileOrder;

  /**
   * Location of the incremental compile will install compiled compiler bridge jars. Default is
   * sbt's "~/.sbt/1.0/zinc/org.scala-sbt".
   */
  @Parameter(property = "secondaryCacheDir")
  private File secondaryCacheDir;

  protected abstract File getOutputDir() throws Exception;

  protected abstract Set<File> getClasspathElements() throws Exception;

  private long _lastCompileAt = -1;

  private SbtIncrementalCompiler incremental;

  /** Analysis cache file for incremental recompilation. */
  protected abstract File getAnalysisCacheFile() throws Exception;

  @Override
  protected void doExecute() throws Exception {
    if (getLog().isDebugEnabled()) {
      for (File directory : getSourceDirectories()) {
        getLog().debug(FileUtils.pathOf(directory, useCanonicalPath));
      }
    }
    File outputDir = FileUtils.fileOf(getOutputDir(), useCanonicalPath);
    File analysisCacheFile = FileUtils.fileOf(getAnalysisCacheFile(), useCanonicalPath);
    int nbFiles =
        compile(
            getSourceDirectories(), outputDir, analysisCacheFile, getClasspathElements(), false);
    if (hasCompileErrors()) {
      throw new MojoFailureException("scala compilation failed");
    }
    switch (nbFiles) {
      case -1:
        getLog().info("No sources to compile");
        break;
      case 0:
        getLog().info("Nothing to compile - all classes are up to date");
        break;
      default:
        break;
    }
  }

  protected JavaMainCaller getScalaCommand() throws Exception {
    Context sc = findScalaContext();
    return getScalaCommand(fork, sc.compilerMainClassName(scalaClassName, false));
  }

  protected int compile(
      List<File> sourceRootDirs,
      File outputDir,
      File analysisCacheFile,
      Set<File> classpathElements,
      boolean compileInLoop)
      throws Exception {
    if (!compileInLoop && recompileMode == RecompileMode.incremental) {
      // if not compileInLoop, invoke incrementalCompile immediately
      long n0 = System.nanoTime();
      int res =
          incrementalCompile(
              classpathElements, sourceRootDirs, outputDir, analysisCacheFile, false);
      getLog().info(String.format("compile in %.1f s", (System.nanoTime() - n0) / 1_000_000_000.0));
      return res;
    }

    long t0 = System.currentTimeMillis();
    long n0 = System.nanoTime();
    LastCompilationInfo lastCompilationInfo = LastCompilationInfo.find(sourceRootDirs, outputDir);
    if (_lastCompileAt < 0) {
      _lastCompileAt = lastCompilationInfo.getLastSuccessfulTS();
    }

    List<File> files = getFilesToCompile(sourceRootDirs, _lastCompileAt);

    if (files == null) {
      return -1;
    }

    if (files.size() < 1) {
      return 0;
    }
    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }
    long n1 = System.nanoTime();
    long t1 = t0 + ((n1 - n0) / 1_000_000);

    if (compileInLoop && recompileMode == RecompileMode.incremental) {
      // if compileInLoop, do not invoke incrementalCompile when there's no change
      int retCode =
          incrementalCompile(classpathElements, sourceRootDirs, outputDir, analysisCacheFile, true);
      _lastCompileAt = t1;
      if (retCode == 1) {
        lastCompilationInfo.setLastSuccessfulTS(t1);
      }
      return retCode;
    }

    getLog()
        .info(
            String.format(
                "Compiling %d source files to %s at %d",
                files.size(), outputDir.getAbsolutePath(), t1));
    JavaMainCaller jcmd = getScalaCommand();
    jcmd.redirectToLog();
    if (!classpathElements.isEmpty())
      jcmd.addArgs("-classpath", FileUtils.toMultiPath(classpathElements));
    jcmd.addArgs("-d", outputDir.getAbsolutePath());
    // jcmd.addArgs("-sourcepath", sourceDir.getAbsolutePath());
    for (File f : files) {
      jcmd.addArgs(f.getAbsolutePath());
    }
    try {
      if (jcmd.run(displayCmd, !compileInLoop)) {
        lastCompilationInfo.setLastSuccessfulTS(t1);
      } else {
        compileErrors = true;
      }
    } catch (Exception e) {
      compileErrors = true;
      getLog().error("exception compilation error occurred!!!", e);
    }
    getLog().info(String.format("prepare-compile in %.1f s", (n1 - n0) / 1_000_000_000.0));
    getLog().info(String.format("compile in %.1f s", (System.nanoTime() - n1) / 1_000_000_000.0));
    _lastCompileAt = t1;
    return files.size();
  }

  /** Returns true if the previous compile failed */
  boolean hasCompileErrors() {
    return compileErrors;
  }

  void clearCompileErrors() {
    compileErrors = false;
  }

  private List<File> getFilesToCompile(List<File> sourceRootDirs, long lastSuccessfulCompileTime)
      throws Exception {
    List<File> sourceFiles = findSourceWithFilters(sourceRootDirs);
    if (sourceFiles.size() == 0) {
      return null;
    }

    // filter uptodate
    // filter is not applied to .java, because scalac failed to used existing .class
    // for unmodified .java
    // failed with "error while loading Xxx, class file
    // '.../target/classes/.../Xxxx.class' is broken"
    // (restore how it work in 2.11 and failed in 2.12)
    // TODO a better behavior : if there is at least one .scala to compile then add
    // all .java, if there is at least one .java then add all .scala (because we
    // don't manage class dependency)
    List<File> files = new ArrayList<>(sourceFiles.size());
    if (_lastCompileAt > 0
        || (recompileMode != RecompileMode.all && (lastSuccessfulCompileTime > 0))) {
      ArrayList<File> modifiedScalaFiles = new ArrayList<>(sourceFiles.size());
      ArrayList<File> modifiedJavaFiles = new ArrayList<>(sourceFiles.size());
      for (File f : sourceFiles) {
        if (f.lastModified() >= lastSuccessfulCompileTime) {
          if (f.getName().endsWith(".java")) {
            modifiedJavaFiles.add(f);
          } else {
            modifiedScalaFiles.add(f);
          }
        }
      }
      if ((modifiedScalaFiles.size() != 0) || (modifiedJavaFiles.size() != 0)) {
        files.addAll(sourceFiles);
        notifyCompilation(sourceRootDirs);
      }
    } else {
      files.addAll(sourceFiles);
      notifyCompilation(sourceRootDirs);
    }
    return files;
  }

  private void notifyCompilation(List<File> files) throws Exception {
    if (notifyCompilation) {
      for (File f : files) {
        getLog()
            .info(String.format("%s:-1: info: compiling", FileUtils.pathOf(f, useCanonicalPath)));
      }
    }
  }

  private static class LastCompilationInfo {
    static LastCompilationInfo find(List<File> sourceRootDirs, File outputDir) {
      StringBuilder hash = new StringBuilder();
      for (File f : sourceRootDirs) {
        hash.append(f.toString());
      }
      return new LastCompilationInfo(
          new File(outputDir.getAbsolutePath() + "." + hash.toString().hashCode() + ".timestamp"),
          outputDir);
    }

    private final File _lastCompileAtFile;
    private final File _outputDir;

    private LastCompilationInfo(File f, File outputDir) {
      _lastCompileAtFile = f;
      _outputDir = outputDir;
    }

    long getLastSuccessfulTS() {
      long back = -1;
      if (_lastCompileAtFile.exists() && _outputDir.exists() && _outputDir.list().length > 0) {
        back = _lastCompileAtFile.lastModified();
      }
      return back;
    }

    void setLastSuccessfulTS(long v) throws Exception {
      if (!_lastCompileAtFile.exists()) {
        FileUtils.fileWrite(_lastCompileAtFile.getAbsolutePath(), ".");
      }
      _lastCompileAtFile.setLastModified(v);
    }
  }

  private ScalaInstance makeScalaInstance(Context sc) throws Exception {
    File[] compilerJars =
        sc.findCompilerAndDependencies().stream()
            .map(Artifact::getFile)
            .collect(Collectors.toList())
            .toArray(new File[] {});
    URL[] compilerJarUrls = FileUtils.toUrls(compilerJars);

    File[] libraryJars =
        sc.findLibraryAndDependencies().stream()
            .map(Artifact::getFile)
            .collect(Collectors.toList())
            .toArray(new File[] {});
    URL[] libraryJarUrls = FileUtils.toUrls(libraryJars);

    SortedSet<File> allJars = new TreeSet<>();
    allJars.addAll(Arrays.asList(compilerJars));
    allJars.addAll(Arrays.asList(libraryJars));
    File[] allJarFiles = allJars.toArray(new File[] {});

    ClassLoader loaderLibraryOnly =
        new ScalaCompilerLoader(libraryJarUrls, xsbti.Reporter.class.getClassLoader());
    ClassLoader loaderCompilerOnly = new URLClassLoader(compilerJarUrls, loaderLibraryOnly);

    if (getLog().isDebugEnabled()) {
      getLog().debug("compilerJars: " + FileUtils.toMultiPath(compilerJars));
      getLog().debug("libraryJars: " + FileUtils.toMultiPath(libraryJars));
    }

    return new ScalaInstance(
        sc.version().toString(),
        loaderCompilerOnly,
        loaderCompilerOnly,
        loaderLibraryOnly,
        libraryJars,
        compilerJars,
        allJarFiles,
        Option.apply(sc.version().toString()));
  }

  // Incremental compilation
  private int incrementalCompile(
      Set<File> classpathElements,
      List<File> sourceRootDirs,
      File outputDir,
      File cacheFile,
      boolean compileInLoop)
      throws Exception {
    List<File> sources = findSourceWithFilters(sourceRootDirs);
    if (sources.isEmpty()) {
      return -1;
    }

    // TODO - Do we really need this duplicated here?
    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }

    if (incremental == null) {
      Context sc = findScalaContext();
      File javaHome = JavaLocator.findHomeFromToolchain(getToolchain());
      ScalaInstance instance = makeScalaInstance(sc);

      incremental =
          new SbtIncrementalCompiler(
              javaHome,
              new MavenArtifactResolver(factory, session),
              secondaryCacheDir,
              getLog(),
              cacheFile,
              compileOrder,
              instance);
    }

    classpathElements.remove(outputDir);
    List<String> scalacOptions = getScalacOptions();
    List<String> javacOptions = getJavacOptions();

    try {
      incremental.compile(
          classpathElements.stream().map(File::toPath).collect(Collectors.toSet()),
          sources.stream().map(File::toPath).collect(Collectors.toList()),
          outputDir.toPath(),
          scalacOptions,
          javacOptions);
    } catch (xsbti.CompileFailed e) {
      if (compileInLoop) {
        compileErrors = true;
      } else {
        throw e;
      }
    }

    return 1;
  }
}

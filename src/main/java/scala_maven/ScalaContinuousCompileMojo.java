/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package scala_maven;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import scala_maven_executions.JavaMainCaller;
import util.FileUtils;

/**
 * Compile the main and test scala source directory in continuous (infinite loop). !! This is an
 * util goal for commandline usage only (Do not use or call it in a pom) !!!
 */
@Mojo(name = "cc", requiresDependencyResolution = ResolutionScope.TEST)
public class ScalaContinuousCompileMojo extends ScalaCompilerSupport {

  /** The output directory for compilation. */
  @Parameter(property = "project.build.outputDirectory")
  protected File mainOutputDir;

  /**
   * The main directory containing scala source for compilation
   *
   * <p>Note: Allows extending for 3rd-party usages
   */
  @Parameter(defaultValue = "${project.build.sourceDirectory}/../scala")
  protected File mainSourceDir;

  /**
   * The directory to place test compilation output in
   *
   * <p>Note: Allows extending for 3rd-party usages
   */
  @Parameter(defaultValue = "${project.build.testOutputDirectory}")
  protected File testOutputDir;

  /**
   * The directory containing test source for compilation
   *
   * <p>Note: Allows extending for 3rd-party usages
   */
  @Parameter(defaultValue = "${project.build.testSourceDirectory}/../scala")
  protected File testSourceDir;

  /**
   * Analysis cache file for incremental recompilation.
   *
   * <p>Note: Allows extending for 3rd-party usages
   */
  @Parameter(
      property = "analysisCacheFile",
      defaultValue = "${project.build.directory}/analysis/compile")
  protected File analysisCacheFile;

  /**
   * Analysis cache file for incremental recompilation.
   *
   * <p>Note: Allows extending for 3rd-party usages
   */
  @Parameter(
      property = "testAnalysisCacheFile",
      defaultValue = "${project.build.directory}/analysis/test-compile")
  protected File testAnalysisCacheFile;

  /**
   * Define if fsc should be used, else scalac is used. fsc => scala.tools.nsc.CompileClient, scalac
   * =&gt; scala.tools.nsc.Main.
   *
   * <p>Note: Allows extending for 3rd-party usages
   */
  @Parameter(property = "fsc", defaultValue = "true")
  protected boolean useFsc;

  /**
   * Define if cc should run once or in infinite loop. (useful for test or working with editor)
   *
   * <p>Note: Allows extending for 3rd-party usages
   */
  @Parameter(property = "once", defaultValue = "false")
  protected boolean once;

  /**
   * Turns verbose output on.
   *
   * <p>Note: Allows extending for 3rd-party usages
   */
  @Parameter(property = "verbose", defaultValue = "false")
  protected boolean verbose;

  @Override
  protected Set<File> getClasspathElements() {
    throw new UnsupportedOperationException("USELESS");
  }

  @Override
  protected File getOutputDir() {
    throw new UnsupportedOperationException("USELESS");
  }

  @Override
  protected List<File> getSourceDirectories() {
    throw new UnsupportedOperationException("USELESS");
  }

  @Override
  protected File getAnalysisCacheFile() {
    throw new UnsupportedOperationException("USELESS");
  }

  @Override
  protected JavaMainCaller getScalaCommand() throws Exception {
    JavaMainCaller jcmd = super.getScalaCommand();
    if (useFsc && verbose) {
      jcmd.addOption("-verbose", true);
    }
    return jcmd;
  }

  @Override
  protected final void doExecute() throws Exception {

    mainOutputDir = FileUtils.fileOf(mainOutputDir, useCanonicalPath);
    if (!mainOutputDir.exists()) {
      mainOutputDir.mkdirs();
    }

    List<String> mainSources = new ArrayList<>(project.getCompileSourceRoots());
    mainSources.add(FileUtils.pathOf(mainSourceDir, useCanonicalPath));
    List<File> mainSourceDirs = normalize(mainSources);

    testOutputDir = FileUtils.fileOf(testOutputDir, useCanonicalPath);
    if (!testOutputDir.exists()) {
      testOutputDir.mkdirs();
    }

    List<String> testSources = new ArrayList<>(project.getTestCompileSourceRoots());
    testSources.add(FileUtils.pathOf(testSourceDir, useCanonicalPath));
    List<File> testSourceDirs = normalize(testSources);

    analysisCacheFile = FileUtils.fileOf(analysisCacheFile, useCanonicalPath);
    testAnalysisCacheFile = FileUtils.fileOf(testAnalysisCacheFile, useCanonicalPath);

    if (useFsc && recompileMode != RecompileMode.incremental) {
      getLog().info("use fsc for compilation");
      scalaClassName = "scala.tools.nsc.CompileClient";
      if (!once) {
        StopServer stopServer = new StopServer();
        stopServer.run();
        startNewCompileServer();
        Runtime.getRuntime().addShutdownHook(stopServer);
      } else {
        startNewCompileServer();
      }
    }

    getLog().info("wait for files to compile...");
    do {
      clearCompileErrors();

      int nbFile = 0;
      if (!mainSourceDirs.isEmpty()) {
        nbFile =
            compile(
                mainSourceDirs,
                mainOutputDir,
                analysisCacheFile,
                FileUtils.fromStrings(project.getCompileClasspathElements()),
                true);
        // If there are no source files, the compile method returns -1. Thus, to make
        // sure we
        // still run the tests if there are test sources, reset nbFile to zero.
        if (nbFile == -1) nbFile = 0;
      }
      if (!testSourceDirs.isEmpty()) {
        nbFile +=
            compile(
                testSourceDirs,
                testOutputDir,
                testAnalysisCacheFile,
                FileUtils.fromStrings(project.getTestClasspathElements()),
                true);
      }
      if (nbFile > 0) {
        if (!hasCompileErrors()) {
          postCompileActions();
        } else {
          getLog().info("Not running test cases due to compile error");
        }
      }
      if (!once) {
        if (nbFile > 0) {
          getLog().info("wait for files to compile...");
          Thread.sleep(5000);
        } else {
          Thread.sleep(3000);
        }
      }
    } while (!once);
  }

  /**
   * Allows derived Mojos to do things after a compile has succesfully completed such as run test
   * cases
   */
  protected void postCompileActions() throws Exception {}

  private void startNewCompileServer() throws Exception {
    File serverTagFile = new File(mainOutputDir + ".server");
    if (serverTagFile.exists()) {
      return;
    }
    getLog().info("start server...");
    JavaMainCaller jcmd = getEmptyScalaCommand("scala.tools.nsc.MainGenericRunner");
    jcmd.addArgs("scala.tools.nsc.CompileServer");
    jcmd.addJvmArgs(jvmArgs);
    jcmd.addArgs(args);
    jcmd.spawn(displayCmd);
    FileUtils.fileWrite(serverTagFile.getAbsolutePath(), ".");
    Thread.sleep(1000); // HACK To wait startup time of server (avoid first fsc command to failed to
    // contact server)
  }

  private class StopServer extends Thread {
    @Override
    public void run() {
      try {
        getLog().info("stop server(s)...");
        JavaMainCaller jcmd = getScalaCommand();
        jcmd.addArgs("-shutdown");
        jcmd.run(displayCmd, false);
        File serverTagFile = new File(mainOutputDir + ".server");
        if (serverTagFile.exists()) {
          serverTagFile.delete();
        }
      } catch (Exception exc) {
        // getLog().warn(exc);
      }
    }
  }
}

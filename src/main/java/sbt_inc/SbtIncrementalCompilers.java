/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package sbt_inc;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.exec.LogOutputStream;
import org.apache.maven.plugin.logging.Log;
import sbt.internal.inc.*;
import sbt.internal.inc.ScalaInstance;
import sbt.util.Logger;
import scala_maven.MavenArtifactResolver;
import scala_maven.VersionNumber;
import scala_maven_executions.Fork;
import scala_maven_executions.ForkLogger;
import xsbti.compile.*;

public final class SbtIncrementalCompilers {

  private static File COMPILER_BRIDGE_JAR = null;
  private static SbtIncrementalCompiler SBT_INCREMENTAL_COMPILER = null;

  public static SbtIncrementalCompiler make(
      File javaHome,
      MavenArtifactResolver resolver,
      File secondaryCacheDir,
      Log mavenLogger,
      VersionNumber scalaVersion,
      Collection<File> compilerAndDependencies,
      Collection<File> libraryAndDependencies,
      String[] jvmArgs,
      File javaExec,
      List<File> forkBootClasspath)
      throws Exception {

    if (COMPILER_BRIDGE_JAR == null) {
      if (mavenLogger.isInfoEnabled()) {
        mavenLogger.info("Building compiler bridge JAR");
      }

      ScalaInstance scalaInstance =
          ScalaInstances.makeScalaInstance(
              scalaVersion.toString(), compilerAndDependencies, libraryAndDependencies);

      COMPILER_BRIDGE_JAR =
          CompilerBridgeFactory.getCompiledBridgeJar(
              scalaVersion, scalaInstance, secondaryCacheDir, resolver, mavenLogger);
    } else {
      if (mavenLogger.isInfoEnabled()) {
        mavenLogger.info("Reusing compiler bridge JAR: " + COMPILER_BRIDGE_JAR.getAbsolutePath());
      }
    }

    if (jvmArgs == null || jvmArgs.length == 0) {
      return makeInProcess(
          javaHome,
          COMPILER_BRIDGE_JAR,
          new MavenLoggerSbtAdapter(mavenLogger),
          scalaVersion.toString(),
          compilerAndDependencies,
          libraryAndDependencies);
    } else {
      return makeForkedProcess(
          javaHome,
          COMPILER_BRIDGE_JAR,
          scalaVersion.toString(),
          compilerAndDependencies,
          libraryAndDependencies,
          mavenLogger,
          jvmArgs,
          javaExec,
          forkBootClasspath);
    }
  }

  static SbtIncrementalCompiler makeInProcess(
      File javaHome,
      File compilerBridgeJar,
      Logger sbtLogger,
      String scalaVersion,
      Collection<File> compilerAndDependencies,
      Collection<File> libraryAndDependencies) {

    if (SBT_INCREMENTAL_COMPILER == null) {
      try {
        SBT_INCREMENTAL_COMPILER =
            new InProcessSbtIncrementalCompiler(
                javaHome,
                compilerBridgeJar,
                sbtLogger,
                scalaVersion,
                compilerAndDependencies,
                libraryAndDependencies);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    return SBT_INCREMENTAL_COMPILER;
  }

  private static SbtIncrementalCompiler makeForkedProcess(
      File javaHome,
      File compilerBridgeJar,
      String scalaVersion,
      Collection<File> compilerAndDependencies,
      Collection<File> libraryAndDependencies,
      Log mavenLogger,
      String[] jvmArgs,
      File javaExec,
      List<File> pluginArtifacts) {

    List<String> forkClasspath =
        pluginArtifacts.stream().map(File::getPath).collect(Collectors.toList());

    return (classpathElements,
        sources,
        classesDirectory,
        scalacOptions,
        javacOptions,
        compileOrder,
        cacheFile) -> {
      try {
        String[] args =
            new ForkedSbtIncrementalCompilerMain.Args(
                    javaHome,
                    cacheFile,
                    compileOrder,
                    compilerBridgeJar,
                    scalaVersion,
                    compilerAndDependencies,
                    libraryAndDependencies,
                    classpathElements,
                    sources,
                    classesDirectory,
                    scalacOptions,
                    javacOptions,
                    mavenLogger.isDebugEnabled())
                .generateArgs();

        Fork fork =
            new Fork(
                ForkedSbtIncrementalCompilerMain.class.getName(),
                forkClasspath,
                jvmArgs,
                args,
                javaExec);

        fork.run(
            new LogOutputStream() {
              private final ForkLogger forkLogger =
                  new ForkLogger() {
                    @Override
                    public void onException(Exception t) {
                      mavenLogger.error(t);
                    }

                    @Override
                    public void onError(String content) {
                      mavenLogger.error(content);
                    }

                    @Override
                    public void onWarn(String content) {
                      mavenLogger.warn(content);
                    }

                    @Override
                    public void onInfo(String content) {
                      mavenLogger.info(content);
                    }

                    @Override
                    public void onDebug(String content) {
                      mavenLogger.debug(content);
                    }
                  };

              @Override
              protected void processLine(String line, int level) {
                forkLogger.processLine(line);
              }

              public void close() throws IOException {
                forkLogger.forceNextLineToFlush();
                super.close();
              }
            });
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    };
  }
}

/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package sbt_inc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.exec.LogOutputStream;
import org.apache.maven.plugin.logging.Log;
import sbt.internal.inc.*;
import sbt.internal.inc.FileAnalysisStore;
import sbt.internal.inc.ScalaInstance;
import sbt.util.Logger;
import scala.Option;
import scala.jdk.FunctionWrappers;
import scala_maven.MavenArtifactResolver;
import scala_maven.VersionNumber;
import scala_maven_executions.Fork;
import scala_maven_executions.ForkLogger;
import xsbti.PathBasedFile;
import xsbti.T2;
import xsbti.VirtualFile;
import xsbti.compile.*;

public final class SbtIncrementalCompilers {
  public static SbtIncrementalCompiler make(
      File javaHome,
      MavenArtifactResolver resolver,
      File secondaryCacheDir,
      Log mavenLogger,
      File cacheFile,
      CompileOrder compileOrder,
      VersionNumber scalaVersion,
      Collection<File> compilerAndDependencies,
      Collection<File> libraryAndDependencies,
      String[] jvmArgs,
      File javaExec,
      List<File> forkBootClasspath)
      throws Exception {

    ScalaInstance scalaInstance =
        ScalaInstances.makeScalaInstance(
            scalaVersion.toString(), compilerAndDependencies, libraryAndDependencies);

    File compilerBridgeJar =
        CompilerBridgeFactory.getCompiledBridgeJar(
            scalaVersion, scalaInstance, secondaryCacheDir, resolver, mavenLogger);

    if (jvmArgs == null || jvmArgs.length == 0) {
      return makeInProcess(
          javaHome,
          cacheFile,
          compileOrder,
          scalaInstance,
          compilerBridgeJar,
          new MavenLoggerSbtAdapter(mavenLogger));
    } else {
      return makeForkedProcess(
          javaHome,
          cacheFile,
          compileOrder,
          compilerBridgeJar,
          scalaVersion,
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
      File cacheFile,
      CompileOrder compileOrder,
      ScalaInstance scalaInstance,
      File compilerBridgeJar,
      Logger sbtLogger) {

    Compilers compilers = makeCompilers(scalaInstance, javaHome, compilerBridgeJar);
    AnalysisStore analysisStore = AnalysisStore.getCachedStore(FileAnalysisStore.binary(cacheFile));
    Setup setup = makeSetup(cacheFile, sbtLogger);
    IncrementalCompiler compiler = ZincUtil.defaultIncrementalCompiler();

    return new InProcessSbtIncrementalCompiler(
        compilers, analysisStore, setup, compiler, compileOrder, sbtLogger);
  }

  private static SbtIncrementalCompiler makeForkedProcess(
      File javaHome,
      File cacheFile,
      CompileOrder compileOrder,
      File compilerBridgeJar,
      VersionNumber scalaVersion,
      Collection<File> compilerAndDependencies,
      Collection<File> libraryAndDependencies,
      Log mavenLogger,
      String[] jvmArgs,
      File javaExec,
      List<File> pluginArtifacts) {

    List<String> forkClasspath =
        pluginArtifacts.stream().map(File::getPath).collect(Collectors.toList());

    return (classpathElements, sources, classesDirectory, scalacOptions, javacOptions) -> {
      try {
        String[] args =
            new ForkedSbtIncrementalCompilerMain.Args(
                    javaHome,
                    cacheFile,
                    compileOrder,
                    compilerBridgeJar,
                    scalaVersion.toString(),
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

  private static Compilers makeCompilers(
      ScalaInstance scalaInstance, File javaHome, File compilerBridgeJar) {
    ScalaCompiler scalaCompiler =
        new AnalyzingCompiler(
            scalaInstance, // scalaInstance
            ZincCompilerUtil.constantBridgeProvider(scalaInstance, compilerBridgeJar), // provider
            ClasspathOptionsUtil.auto(), // classpathOptions
            new FunctionWrappers.FromJavaConsumer<>(noop -> {}), // onArgsHandler
            Option.apply(null) // classLoaderCache
            );

    return ZincUtil.compilers(
        scalaInstance, ClasspathOptionsUtil.boot(), Option.apply(javaHome.toPath()), scalaCompiler);
  }

  private static Setup makeSetup(File cacheFile, xsbti.Logger sbtLogger) {
    PerClasspathEntryLookup lookup =
        new PerClasspathEntryLookup() {
          @Override
          public Optional<CompileAnalysis> analysis(VirtualFile classpathEntry) {
            Path path = ((PathBasedFile) classpathEntry).toPath();

            String analysisStoreFileName = null;
            if (Files.isDirectory(path)) {
              if (path.getFileName().toString().equals("classes")) {
                analysisStoreFileName = "compile";

              } else if (path.getFileName().toString().equals("test-classes")) {
                analysisStoreFileName = "test-compile";
              }
            }

            if (analysisStoreFileName != null) {
              File analysisStoreFile =
                  path.getParent().resolve("analysis").resolve(analysisStoreFileName).toFile();
              if (analysisStoreFile.exists()) {
                return AnalysisStore.getCachedStore(FileAnalysisStore.binary(analysisStoreFile))
                    .get()
                    .map(AnalysisContents::getAnalysis);
              }
            }
            return Optional.empty();
          }

          @Override
          public DefinesClass definesClass(VirtualFile classpathEntry) {
            return classpathEntry.name().equals("rt.jar")
                ? className -> false
                : Locate.definesClass(classpathEntry);
          }
        };

    return Setup.of(
        lookup, // lookup
        false, // skip
        cacheFile, // cacheFile
        CompilerCache.fresh(), // cache
        IncOptions.of(), // incOptions
        new LoggedReporter(100, sbtLogger, pos -> pos), // reporter
        Optional.empty(), // optionProgress
        new T2[] {});
  }
}

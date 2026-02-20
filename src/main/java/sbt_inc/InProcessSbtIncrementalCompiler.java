/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package sbt_inc;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import sbt.internal.inc.*;
import sbt.internal.inc.FileAnalysisStore;
import sbt.internal.inc.ScalaInstance;
import scala.Option;
import scala.jdk.FunctionWrappers;
import xsbti.Logger;
import xsbti.PathBasedFile;
import xsbti.T2;
import xsbti.VirtualFile;
import xsbti.compile.*;

public final class InProcessSbtIncrementalCompiler implements SbtIncrementalCompiler {
  private final Compilers compilers;
  private final IncrementalCompiler compiler;
  private final Logger sbtLogger;

  public InProcessSbtIncrementalCompiler(
      File javaHome,
      File compilerBridgeJar,
      Logger sbtLogger,
      String scalaVersion,
      Collection<File> compilerAndDependencies,
      Collection<File> libraryAndDependencies) {

    this.sbtLogger = sbtLogger;

    ScalaInstance scalaInstance =
        ScalaInstances.makeScalaInstance(
            scalaVersion, compilerAndDependencies, libraryAndDependencies);

    compilers = makeCompilers(scalaInstance, javaHome, compilerBridgeJar);
    compiler = ZincUtil.defaultIncrementalCompiler();
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

  @Override
  public void compile(
      Collection<File> classpathElements,
      Collection<File> sources,
      File classesDirectory,
      Collection<String> scalacOptions,
      Collection<String> javacOptions,
      CompileOrder compileOrder,
      File cacheFile) {

    // incremental compiler needs to add the output dir in the classpath for Java + Scala
    Collection<File> fullClasspathElements = new ArrayList<>(classpathElements);
    fullClasspathElements.add(classesDirectory);

    CompileOptions options =
        CompileOptions.of(
            fullClasspathElements.stream()
                .map(file -> new PlainVirtualFile(file.toPath()))
                .toArray(VirtualFile[]::new), // classpath
            sources.stream()
                .map(file -> new PlainVirtualFile(file.toPath()))
                .toArray(VirtualFile[]::new), // sources
            classesDirectory.toPath(), //
            scalacOptions.toArray(new String[] {}), // scalacOptions
            javacOptions.toArray(new String[] {}), // javacOptions
            100, // maxErrors
            pos -> pos, // sourcePositionMappers
            compileOrder, // order
            Optional.empty(), // temporaryClassesDirectory
            Optional.empty(), // _converter
            Optional.empty(), // _stamper
            Optional.empty() // _earlyOutput
            );

    AnalysisStore analysisStore = AnalysisStore.getCachedStore(FileAnalysisStore.binary(cacheFile));
    Inputs inputs =
        Inputs.of(
            compilers, options, makeSetup(cacheFile, sbtLogger), previousResult(analysisStore));

    CompileResult newResult = compiler.compile(inputs, sbtLogger);

    analysisStore.set(AnalysisContents.create(newResult.analysis(), newResult.setup()));
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

  private PreviousResult previousResult(AnalysisStore analysisStore) {
    Optional<AnalysisContents> analysisContents = analysisStore.get();
    if (analysisContents.isPresent()) {
      AnalysisContents analysisContents0 = analysisContents.get();
      CompileAnalysis previousAnalysis = analysisContents0.getAnalysis();
      MiniSetup previousSetup = analysisContents0.getMiniSetup();
      return PreviousResult.of(Optional.of(previousAnalysis), Optional.of(previousSetup));
    } else {
      return PreviousResult.of(Optional.empty(), Optional.empty());
    }
  }
}

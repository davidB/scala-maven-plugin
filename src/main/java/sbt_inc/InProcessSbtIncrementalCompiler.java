/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package sbt_inc;

import java.io.File;
import java.util.*;
import sbt.internal.inc.*;
import xsbti.Logger;
import xsbti.VirtualFile;
import xsbti.compile.*;

public final class InProcessSbtIncrementalCompiler implements SbtIncrementalCompiler {
  private final Compilers compilers;
  private final AnalysisStore analysisStore;
  private final Setup setup;

  private final IncrementalCompiler compiler;
  private final CompileOrder compileOrder;
  private final Logger sbtLogger;

  public InProcessSbtIncrementalCompiler(
      Compilers compilers,
      AnalysisStore analysisStore,
      Setup setup,
      IncrementalCompiler compiler,
      CompileOrder compileOrder,
      Logger sbtLogger) {
    this.compilers = compilers;
    this.analysisStore = analysisStore;
    this.setup = setup;
    this.compiler = compiler;
    this.compileOrder = compileOrder;
    this.sbtLogger = sbtLogger;
  }

  @Override
  public void compile(
      Collection<File> classpathElements,
      Collection<File> sources,
      File classesDirectory,
      Collection<String> scalacOptions,
      Collection<String> javacOptions) {

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

    Inputs inputs = Inputs.of(compilers, options, setup, previousResult());

    CompileResult newResult = compiler.compile(inputs, sbtLogger);
    analysisStore.set(AnalysisContents.create(newResult.analysis(), newResult.setup()));
  }

  private PreviousResult previousResult() {
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

/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package sbt_inc;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import sbt.util.Level;
import sbt.util.Logger;
import scala.Enumeration;
import scala.Function0;
import scala_maven_executions.ForkLogLevel;
import xsbti.compile.*;

public final class ForkedSbtIncrementalCompilerMain {

  public static final class Args {
    public final File javaHome;
    public final File cacheFile;
    public final CompileOrder compileOrder;
    public final File compilerBridgeJar;
    public final String scalaVersion;
    public final Collection<File> compilerAndDependencies;
    public final Collection<File> libraryAndDependencies;

    public final Collection<File> classpathElements;
    public final Collection<File> sources;
    public final File classesDirectory;
    public final Collection<String> scalacOptions;
    public final Collection<String> javacOptions;

    public final boolean debugEnabled;

    public Args(
        File javaHome,
        File cacheFile,
        CompileOrder compileOrder,
        File compilerBridgeJar,
        String scalaVersion,
        Collection<File> compilerAndDependencies,
        Collection<File> libraryAndDependencies,
        Collection<File> classpathElements,
        Collection<File> sources,
        File classesDirectory,
        Collection<String> scalacOptions,
        Collection<String> javacOptions,
        boolean debugEnabled) {
      this.javaHome = javaHome;
      this.cacheFile = cacheFile;
      this.compileOrder = compileOrder;
      this.compilerBridgeJar = compilerBridgeJar;
      this.scalaVersion = scalaVersion;
      this.compilerAndDependencies = compilerAndDependencies;
      this.libraryAndDependencies = libraryAndDependencies;
      this.classpathElements = classpathElements;
      this.sources = sources;
      this.classesDirectory = classesDirectory;
      this.scalacOptions = scalacOptions;
      this.javacOptions = javacOptions;
      this.debugEnabled = debugEnabled;
    }

    private <T> void writeCollection(
        List<String> args, Collection<T> collection, Function<T, String> f) {
      args.add(String.valueOf(collection.size()));
      for (T entry : collection) {
        args.add(f.apply(entry));
      }
    }

    public String[] generateArgs() {
      List<String> args = new ArrayList<>();
      args.add(javaHome.toString());
      args.add(cacheFile.getPath());
      args.add(compileOrder.name());
      args.add(compilerBridgeJar.getPath());
      args.add(scalaVersion);
      writeCollection(args, compilerAndDependencies, File::getPath);
      writeCollection(args, libraryAndDependencies, File::getPath);
      writeCollection(args, classpathElements, File::getPath);
      writeCollection(args, sources, File::getPath);
      args.add(classesDirectory.toString());
      writeCollection(args, scalacOptions, Function.identity());
      writeCollection(args, javacOptions, Function.identity());
      args.add(String.valueOf(debugEnabled));
      return args.toArray(new String[] {});
    }

    private static <T> List<T> readList(String[] args, AtomicInteger index, Function<String, T> f) {
      int size = Integer.parseInt(args[index.getAndIncrement()]);
      List<T> list = new ArrayList<>(size);
      for (int i = 0; i < size; i++) {
        list.add(f.apply(args[index.getAndIncrement()]));
      }
      return list;
    }

    static Args parseArgs(String[] args) {
      AtomicInteger index = new AtomicInteger();

      File javaHome = new File(args[index.getAndIncrement()]);
      File cacheFile = new File(args[index.getAndIncrement()]);
      CompileOrder compileOrder = CompileOrder.valueOf(args[index.getAndIncrement()]);
      File compilerBridgeJar = new File(args[index.getAndIncrement()]);
      String scalaVersion = args[index.getAndIncrement()];
      List<File> compilerAndDependencies = readList(args, index, File::new);
      List<File> libraryAndDependencies = readList(args, index, File::new);
      List<File> classpathElements = readList(args, index, File::new);
      List<File> sources = readList(args, index, File::new);
      File classesDirectory = new File(args[index.getAndIncrement()]);
      List<String> scalacOptions = readList(args, index, Function.identity());
      List<String> javacOptions = readList(args, index, Function.identity());
      boolean debugEnabled = Boolean.parseBoolean(args[index.getAndIncrement()]);

      return new Args(
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
          debugEnabled);
    }
  }

  public static void main(String[] args) {
    Args parsedArgs = Args.parseArgs(args);

    Logger sbtLogger =
        new Logger() {
          @Override
          public void log(Enumeration.Value level, Function0<String> message) {
            ForkLogLevel forkLogLevel = null;
            if (level.equals(Level.Error())) {
              forkLogLevel = ForkLogLevel.ERROR;
            } else if (level.equals(Level.Warn())) {
              forkLogLevel = ForkLogLevel.WARN;
            } else if (level.equals(Level.Info())) {
              forkLogLevel = ForkLogLevel.INFO;
            } else if (level.equals(Level.Debug()) && parsedArgs.debugEnabled) {
              forkLogLevel = ForkLogLevel.DEBUG;
            }

            if (forkLogLevel != null) {
              System.out.println(forkLogLevel.addHeader(message.apply()));
            }
          }

          @Override
          public void success(Function0<String> message) {
            log(Level.Info(), message);
          }

          @Override
          public void trace(Function0<Throwable> t) {}
        };

    SbtIncrementalCompiler incrementalCompiler =
        SbtIncrementalCompilers.makeInProcess(
            parsedArgs.javaHome,
            parsedArgs.compilerBridgeJar,
            sbtLogger,
            parsedArgs.scalaVersion,
            parsedArgs.compilerAndDependencies,
            parsedArgs.libraryAndDependencies);

    incrementalCompiler.compile(
        parsedArgs.classpathElements,
        parsedArgs.sources,
        parsedArgs.classesDirectory,
        parsedArgs.scalacOptions,
        parsedArgs.javacOptions,
        parsedArgs.compileOrder,
        parsedArgs.cacheFile);
  }
}

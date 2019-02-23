package sbt_inc;

import scala.compat.java8.functionConverterImpls.*;

import org.apache.maven.plugin.logging.Log;
import sbt.internal.inc.*;
import sbt.internal.inc.FileAnalysisStore;
import sbt.internal.inc.ScalaInstance;
import sbt.internal.inc.classpath.ClasspathUtilities;
import scala.Option;
import scala_maven.VersionNumber;
import xsbti.Logger;
import xsbti.T2;
import xsbti.compile.*;
import xsbti.compile.AnalysisStore;
import xsbti.compile.CompilerCache;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class SbtIncrementalCompiler {

    public static final String SBT_GROUP_ID = "org.scala-sbt";
    public static final String ZINC_ARTIFACT_ID = "zinc";
    public static final String COMPILER_BRIDGE_ARTIFACT_ID = "compiler-bridge";

    private final Logger logger;
    private final IncrementalCompilerImpl compiler;
    private final Compilers compilers;
    private final Setup setup;
    private final AnalysisStore analysisStore;

    public SbtIncrementalCompiler(File libraryJar, File reflectJar, File compilerJar, VersionNumber scalaVersion, List<File> extraJars, File compilerBridgeJar, Log l, List<String> args, File cacheFile) throws Exception {
        l.info("Using incremental compilation");
        if (args.size() > 0) l.warn("extra args for zinc are ignored in non-server mode");
        this.logger = new SbtLogger(l);

        List<File> allJars = new ArrayList<>(extraJars);
        allJars.add(libraryJar);
        allJars.add(reflectJar);
        allJars.add(compilerJar);

        ScalaInstance scalaInstance = new ScalaInstance(
            scalaVersion.toString(), // version
            new URLClassLoader(new URL[]{libraryJar.toURI().toURL(), reflectJar.toURI().toURL(), compilerJar.toURI().toURL()}), // loader
            ClasspathUtilities.rootLoader(), // loaderLibraryOnly
            libraryJar, // libraryJar
            compilerJar, // compilerJar
            allJars.toArray(new File[]{}), // allJars
            Option.apply(scalaVersion.toString()) // explicitActual
        );

        compiler = new IncrementalCompilerImpl();

        AnalyzingCompiler scalaCompiler = new AnalyzingCompiler(
            scalaInstance, // scalaInstance
            ZincCompilerUtil.constantBridgeProvider(scalaInstance, compilerBridgeJar), //provider
            ClasspathOptionsUtil.auto(), // classpathOptions
            new FromJavaConsumer<>(noop -> {
            }), //FIXME foo -> {}, // onArgsHandler
            Option.apply(null) // classLoaderCache
        );

        compilers = compiler.compilers(scalaInstance, ClasspathOptionsUtil.boot(), Option.apply(null), scalaCompiler);

        PerClasspathEntryLookup lookup = new PerClasspathEntryLookup() {
            @Override
            public Optional<CompileAnalysis> analysis(File classpathEntry) {
                return Optional.empty();
            }

            @Override
            public DefinesClass definesClass(File classpathEntry) {
                return Locate.definesClass(classpathEntry);
            }
        };

        LoggedReporter reporter = new LoggedReporter(100, logger, pos -> pos);

        analysisStore = AnalysisStore.getCachedStore(FileAnalysisStore.binary(cacheFile));

        setup =
            compiler.setup(
                lookup, // lookup
                false, // skip
                cacheFile, // cacheFile
                CompilerCache.fresh(), // cache
                IncOptions.of(), // incOptions
                reporter, // reporter
                Option.apply(null), // optionProgress
                new T2[]{}
            );
    }

    public void compile(List<String> classpathElements, List<File> sources, File classesDirectory, List<String> scalacOptions, List<String> javacOptions, String compileOrder) {

        Inputs inputs = compiler.inputs(
            classpathElements.stream().map(File::new).toArray(size -> new File[size]), //classpath
            sources.toArray(new File[]{}), // sources
            classesDirectory, // classesDirectory
            scalacOptions.toArray(new String[]{}), // scalacOptions
            javacOptions.toArray(new String[]{}), // javacOptions
            100, // maxErrors
            new Function[]{}, // sourcePositionMappers
            toCompileOrder(compileOrder), // order
            compilers,
            setup,
            compiler.emptyPreviousResult()
        );

        Optional<AnalysisContents> analysisContents = analysisStore.get();
        if (analysisContents.isPresent()) {
            AnalysisContents analysisContents0 = analysisContents.get();
            CompileAnalysis previousAnalysis = analysisContents0.getAnalysis();
            MiniSetup previousSetup = analysisContents0.getMiniSetup();
            PreviousResult previousResult = PreviousResult.of(Optional.of(previousAnalysis), Optional.of(previousSetup));
            inputs = inputs.withPreviousResult(previousResult);
        }

        CompileResult newResult = compiler.compile(inputs, logger);
        analysisStore.set(AnalysisContents.create(newResult.analysis(), newResult.setup()));
    }

    private CompileOrder toCompileOrder(String name) {
        if (name.equalsIgnoreCase(CompileOrder.Mixed.name())) {
            return CompileOrder.Mixed;
        } else if (name.equalsIgnoreCase(CompileOrder.JavaThenScala.name())) {
            return CompileOrder.JavaThenScala;
        } else if (name.equalsIgnoreCase(CompileOrder.ScalaThenJava.name())) {
            return CompileOrder.ScalaThenJava;
        } else {
            throw new IllegalArgumentException("Unknown compileOrder: " + name);
        }
    }
}

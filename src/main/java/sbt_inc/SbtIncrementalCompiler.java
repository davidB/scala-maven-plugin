package sbt_inc;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import org.apache.maven.plugin.logging.Log;
import sbt.compiler.AnalyzingCompiler;
import sbt.compiler.CompilerCache;
import sbt.compiler.CompilerCache$;
import sbt.compiler.IC;
import sbt.inc.Analysis;
import xsbti.compile.CompileOrder;
import xsbti.compile.DefinesClass;
import xsbti.compile.GlobalsCache;
import xsbti.Maybe;

public class SbtIncrementalCompiler {

    public static final String SBT_GROUP_ID = "org.scala-sbt";
    public static final String COMPILER_INTEGRATION_ARTIFACT_ID = "compiler-integration";
    public static final String COMPILER_INTERFACE_ARTIFACT_ID = "compiler-interface";
    public static final String COMPILER_INTERFACE_CLASSIFIER = "sources";
    public static final String XSBTI_ARTIFACT_ID = "interface";

    private xsbti.Logger logger;

    private SbtCompilers compilers;

    private GlobalsCache compilerCache;

    public SbtIncrementalCompiler(String scalaVersion, File libraryJar, File compilerJar, String sbtVersion, File xsbtiJar, File interfaceJar, int maxCompilers, Log log) throws Exception {
        log.info("Using incremental compilation");
        this.logger = new SbtLogger(log);
        this.compilers = new SbtCompilers(scalaVersion, libraryJar, compilerJar, sbtVersion, xsbtiJar, interfaceJar, logger);
        this.compilerCache = (maxCompilers <= 0) ? CompilerCache.fresh() : CompilerCache$.MODULE$.apply(maxCompilers);
    }

    public void compile(List<String> classpathElements, List<File> sourcesList, File classesDirectory, List<String> scalacOptions, List<String> javacOptions) {
        List<File> fullClasspath = pathsToFiles(classpathElements);
        File[] classpath = fullClasspath.toArray(new File[fullClasspath.size()]);
        File[] sources = sourcesList.toArray(new File[sourcesList.size()]);
        String[] soptions = scalacOptions.toArray(new String[scalacOptions.size()]);
        String[] joptions = javacOptions.toArray(new String[javacOptions.size()]);
        Options options = new Options(classpath, sources, classesDirectory, soptions, joptions);
        Setup setup = new Setup(classesDirectory, compilerCache);
        Inputs inputs = new Inputs(compilers, options, setup);
        IC.compile(inputs, logger);
    }

    public static class Options implements xsbti.compile.Options {

        private File[] classpath;
        private File[] sources;
        private File classesDirectory;
        private String[] scalacOptions;
        private String[] javacOptions;

        public Options(File[] classpath, File[] sources, File classesDirectory, String[] scalacOptions, String[] javacOptions) {
            this.classpath = classpath;
            this.sources = sources;
            this.classesDirectory = classesDirectory;
            this.scalacOptions = scalacOptions;
            this.javacOptions = javacOptions;
        }

        public File[] classpath() {
            return classpath;
        }

        public File[] sources() {
            return sources;
        }

        public File classesDirectory() {
            return classesDirectory;
        }

        public String[] options() {
            return scalacOptions;
        }

        public String[] javacOptions() {
            return javacOptions;
        }

        public int maxErrors() {
            return 100;
        }

        public CompileOrder order() {
            return CompileOrder.Mixed;
        }
    }

    public static class Setup implements xsbti.compile.Setup<Analysis> {

        private File classesDirectory;
        private File cacheFile;
        private GlobalsCache compilerCache;

        public Setup(File classesDirectory, GlobalsCache compilerCache) {
            this.classesDirectory = classesDirectory;
            this.cacheFile = SbtAnalysis.cacheLocation(classesDirectory);
            this.compilerCache = compilerCache;
        }

        // TODO: in-memory cache in front
        public Maybe<Analysis> analysisMap(File file) {
            return SbtAnalysis.analysisMap(file, classesDirectory);
        }

        // TODO: in-memory cache in front
        public DefinesClass definesClass(File file) {
            return SbtLocate.definesClass(file);
        }

        public boolean skip() {
            return false;
        }

        public File cacheFile() {
            return cacheFile;
        }

        public GlobalsCache cache() {
            return compilerCache;
        }
    }

    public static class Inputs implements xsbti.compile.Inputs<Analysis, AnalyzingCompiler> {

        private SbtCompilers compilers;
        private Options options;
        private Setup setup;

        public Inputs(SbtCompilers compilers, Options options, Setup setup) {
            this.compilers = compilers;
            this.options = options;
            this.setup = setup;
        }

        public xsbti.compile.Compilers<AnalyzingCompiler> compilers() {
            return compilers;
        }

        public xsbti.compile.Options options() {
            return options;
        }

        public xsbti.compile.Setup<Analysis> setup() {
            return setup;
        }
    }

    public List<File> pathsToFiles(List<String> paths) {
        List<File> files = new ArrayList<File>(paths.size());
        for (String path : paths) {
            files.add(new File(path));
        }
        return files;
    }
}

package sbt_inc;

import com.typesafe.zinc.Compiler;
import com.typesafe.zinc.*;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.toolchain.Toolchain;
import scala.Option;
import scala_maven_executions.MainHelper;
import util.JavaLocator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SbtIncrementalCompiler {

    public static final String SBT_GROUP_ID = "com.typesafe.sbt";
    public static final String COMPILER_INTEGRATION_ARTIFACT_ID = "incremental-compiler";
    public static final String COMPILER_INTERFACE_ARTIFACT_ID = "compiler-interface";
    public static final String COMPILER_INTERFACE_CLASSIFIER = "sources";
    public static final String XSBTI_ARTIFACT_ID = "sbt-interface";

    private static final String ANALYSIS_MAP_ARG_SEPARATOR = ",";
    private static final String ANALYSIS_MAP_PAIR_SEPARATOR = File.pathSeparator;

    private Log log;

    private ZincClient zinc;

    private boolean useServer = false;

    private File compilerJar;

    private File libraryJar;

    private List<File> extraJars;

    private List<String> extraArgs;

    private xsbti.Logger logger;

    private Compiler compiler;

    public SbtIncrementalCompiler(boolean useZincServer, String zincHost, int zincPort, File libraryJar, File compilerJar, List<File> extraJars, File xsbtiJar, File interfaceJar, Log l, List<String> args) throws Exception {
        this.log = l;
        if (useZincServer) {
            this.zinc = new ZincClient(zincHost, zincPort);
            if (zinc.serverAvailable()) {
                l.info("Using zinc server for incremental compilation");
                this.useServer = true;
                this.compilerJar = compilerJar;
                this.libraryJar = libraryJar;
                this.extraJars = extraJars;
                this.extraArgs = args;
            } else {
                l.warn("Zinc server is not available at port " + zincPort + " - reverting to normal incremental compile");
                this.useServer = false;
            }
        }
        if (!useServer) {
            l.info("Using incremental compilation");
            if (args.size() > 0) l.warn("extra args for zinc are ignored in non-server mode");
            this.logger = new SbtLogger(l);
            Setup setup = Setup.create(compilerJar, libraryJar, extraJars, xsbtiJar, interfaceJar, null, false);
            if (l.isDebugEnabled()) Setup.debug(setup, logger);
            this.compiler = Compiler.create(setup, logger);
        }
    }

    private IncOptions defaultOptions() {
        sbt.inc.IncOptions defaultSbtOptions = sbt.inc.IncOptions.Default();
        return new IncOptions(
                defaultSbtOptions.transitiveStep(),
                defaultSbtOptions.recompileAllFraction(),
                defaultSbtOptions.relationsDebug(),
                defaultSbtOptions.apiDebug(),
                defaultSbtOptions.apiDiffContextSize(),
                defaultSbtOptions.apiDumpDirectory(),
                false,
                Option.<File>empty(),
                defaultSbtOptions.recompileOnMacroDef(),
                defaultSbtOptions.nameHashing());
    }

    public void compile(File baseDir, List<String> classpathElements, List<File> sources, File classesDirectory, List<String> scalacOptions, List<String> javacOptions, File cacheFile, Map<File, File> cacheMap, String compileOrder, Toolchain toolchain) throws Exception {
        if (useServer) {
            zincCompile(baseDir, classpathElements, sources, classesDirectory, scalacOptions, javacOptions, cacheFile, cacheMap, compileOrder, toolchain);
        } else {
            if (log.isDebugEnabled()) log.debug("Incremental compiler = " + compiler + " [" + Integer.toHexString(compiler.hashCode()) + "]");
            List<File> classpath = pathsToFiles(classpathElements);
            Inputs inputs = Inputs.create(classpath, sources, classesDirectory, scalacOptions, javacOptions, cacheFile, cacheMap, compileOrder, defaultOptions(), true);
            if (log.isDebugEnabled()) Inputs.debug(inputs, logger);
            compiler.compile(inputs, logger);
        }
    }

    private void zincCompile(File baseDir, List<String> classpathElements, List<File> sources, File classesDirectory, List<String> scalacOptions, List<String> javacOptions, File cacheFile, Map<File, File> cacheMap, String compileOrder, Toolchain toolchain) throws Exception {
        List<String> arguments = new ArrayList<String>(extraArgs);
        arguments.add("-log-level");
        arguments.add(logLevelToString(log));
        arguments.add("-scala-compiler");
        arguments.add(compilerJar.getAbsolutePath());
        arguments.add("-scala-library");
        arguments.add(libraryJar.getAbsolutePath());
        arguments.add("-scala-extra");
        List<String> extraPaths = new ArrayList<String>();
        for (File extraJar : extraJars) {
            extraPaths.add(extraJar.getAbsolutePath());
        }
        arguments.add(MainHelper.toMultiPath(extraPaths));
        if (!classpathElements.isEmpty()) {
          arguments.add("-classpath");
          arguments.add(MainHelper.toMultiPath(classpathElements));
        }
        arguments.add("-d");
        arguments.add(classesDirectory.getAbsolutePath());
        for (String scalacOption : scalacOptions) {
            arguments.add("-S" + scalacOption);
        }

        String javaHome = JavaLocator.findHomeFromToolchain(toolchain);
        if (javaHome != null) {
            log.info("Toolchain in scala-maven-plugin: " + javaHome);
            arguments.add("-java-home");
            arguments.add(javaHome);
        }

        for (String javacOption : javacOptions) {
            arguments.add("-C" + javacOption);
        }
        arguments.add("-compile-order");
        arguments.add(compileOrder);
        arguments.add("-analysis-cache");
        arguments.add(cacheFile.getAbsolutePath());
        arguments.add("-analysis-map");
        arguments.add(cacheMapToString(cacheMap));
        for (File source : sources) {
            arguments.add(source.getAbsolutePath());
        }

        int exitCode = zinc.run(arguments, baseDir, System.out, System.err);

        if (exitCode != 0) {
            xsbti.Problem[] problems = null;
            throw new sbt.compiler.CompileFailed(arguments.toArray(new String[arguments.size()]), "Compile failed via zinc server", problems);
        }
    }

    private List<File> pathsToFiles(List<String> paths) {
        List<File> files = new ArrayList<File>(paths.size());
        for (String path : paths) {
            files.add(new File(path));
        }
        return files;
    }

    private String logLevelToString(Log l) {
        if (l.isDebugEnabled()) return "debug";
        else if (l.isInfoEnabled()) return "info";
        else if (l.isWarnEnabled()) return "warn";
        else if (l.isErrorEnabled()) return "error";
        else return "info";
    }

    private String cacheMapToString(Map<File, File> cacheMap) throws Exception {
        String analysisMap = "";
        boolean addArgSeparator = false;
        for (Map.Entry<File, File> entry : cacheMap.entrySet()) {
            if (addArgSeparator) analysisMap += ANALYSIS_MAP_ARG_SEPARATOR;
            analysisMap += entry.getKey().getAbsolutePath();
            analysisMap += ANALYSIS_MAP_PAIR_SEPARATOR;
            analysisMap += entry.getValue().getAbsolutePath();
            addArgSeparator = true;
        }
        return analysisMap;
    }
}

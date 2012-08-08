package sbt_inc;

import com.typesafe.zinc.Compiler;
import com.typesafe.zinc.Inputs;
import com.typesafe.zinc.Setup;
import com.typesafe.zinc.ZincClient;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import org.apache.maven.plugin.logging.Log;
import scala_maven_executions.MainHelper;

public class SbtIncrementalCompiler {

    public static final String SBT_GROUP_ID = "com.typesafe.sbt";
    public static final String COMPILER_INTEGRATION_ARTIFACT_ID = "incremental-compiler";
    public static final String COMPILER_INTERFACE_ARTIFACT_ID = "compiler-interface";
    public static final String COMPILER_INTERFACE_CLASSIFIER = "sources";
    public static final String XSBTI_ARTIFACT_ID = "sbt-interface";

    private Log log;

    private ZincClient zinc;

    private boolean useServer = false;

    private File compilerJar;

    private File libraryJar;

    private List<File> extraJars;

    private xsbti.Logger logger;

    private Compiler compiler;

    public SbtIncrementalCompiler(boolean useZincServer, int zincPort, String scalaVersion, File libraryJar, File compilerJar, List<File> extraJars, String sbtVersion, File xsbtiJar, File interfaceJar, Log log) throws Exception {
        this.log = log;
        if (useZincServer) {
            this.zinc = new ZincClient(zincPort);
            if (zinc.serverAvailable()) {
                log.info("Using zinc server for incremental compilation");
                this.useServer = true;
                this.compilerJar = compilerJar;
                this.libraryJar = libraryJar;
                this.extraJars = extraJars;
            } else {
                log.warn("Zinc server is not available at port " + zincPort + " - reverting to normal incremental compile");
                this.useServer = false;
            }
        }
        if (!useServer) {
            log.info("Using incremental compilation");
            this.logger = new SbtLogger(log);
            Setup setup = Setup.create(compilerJar, libraryJar, extraJars, xsbtiJar, interfaceJar, null);
            if (log.isDebugEnabled()) Setup.debug(setup, logger);
            this.compiler = Compiler.create(setup, logger);
        }
    }

    public void compile(File baseDir, List<String> classpathElements, List<File> sources, File classesDirectory, List<String> scalacOptions, List<String> javacOptions, File cacheFile, Map<File, File> cacheMap, String compileOrder) throws Exception {
        if (useServer) {
            zincCompile(baseDir, classpathElements, sources, classesDirectory, scalacOptions, javacOptions, cacheFile, cacheMap, compileOrder);
        } else {
            if (log.isDebugEnabled()) log.debug("Incremental compiler = " + compiler + " [" + Integer.toHexString(compiler.hashCode()) + "]");
            List<File> classpath = pathsToFiles(classpathElements);
            Inputs inputs = Inputs.create(classpath, sources, classesDirectory, scalacOptions, javacOptions, cacheFile, cacheMap, compileOrder);
            if (log.isDebugEnabled()) Inputs.debug(inputs, logger);
            compiler.compile(inputs, logger);
        }
    }

    private void zincCompile(File baseDir, List<String> classpathElements, List<File> sources, File classesDirectory, List<String> scalacOptions, List<String> javacOptions, File cacheFile, Map<File, File> cacheMap, String compileOrder) throws Exception {
        List<String> arguments = new ArrayList<String>();
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
        arguments.add("-classpath");
        arguments.add(MainHelper.toMultiPath(classpathElements));
        arguments.add("-d");
        arguments.add(classesDirectory.getAbsolutePath());
        for (String scalacOption : scalacOptions) {
            arguments.add("-S" + scalacOption);
        }
        for (String javacOption : javacOptions) {
            arguments.add("-J" + javacOption);
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

    private String logLevelToString(Log log) {
        if (log.isDebugEnabled()) return "debug";
        else if (log.isInfoEnabled()) return "info";
        else if (log.isWarnEnabled()) return "warn";
        else if (log.isErrorEnabled()) return "error";
        else return "info";
    }

    private String cacheMapToString(Map<File, File> cacheMap) throws Exception {
        String analysisMap = "";
        boolean addComma = false;
        for (Map.Entry<File, File> entry : cacheMap.entrySet()) {
            if (addComma) analysisMap += ",";
            analysisMap += entry.getKey().getAbsolutePath();
            analysisMap += ":";
            analysisMap += entry.getValue().getAbsolutePath();
            addComma = true;
        }
        return analysisMap;
    }
}

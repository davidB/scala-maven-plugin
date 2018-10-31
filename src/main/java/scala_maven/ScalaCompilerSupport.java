package scala_maven;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import sbt_inc.SbtIncrementalCompiler;
import scala_maven_executions.JavaMainCaller;
import scala_maven_executions.MainHelper;

/**
 * Abstract parent of all Scala Mojo who run compilation
 */
public abstract class ScalaCompilerSupport extends ScalaSourceMojoSupport {

    public static final String ALL = "all";
    public static final String MODIFIED_ONLY = "modified-only";
    public static final String INCREMENTAL = "incremental";

    /**
     * Keeps track of if we get compile errors in incremental mode
     */
    private boolean compileErrors;

    /**
     * Recompile mode to use when sources were previously compiled and there is at
     * least one change:
     * "modified-only" =&gt; only modified sources are recompiled (pre 2.13 behavior),
     * "all" =&gt; all sources are recompiled,
     * "incremental" =&gt; incrementally recompile modified sources and other affected
     * sources.
     *
     */
    @Parameter(property = "recompileMode", defaultValue = "all")
    protected String recompileMode = ALL;

    /**
     * notifyCompilation if true then print a message "path: compiling"
     * for each root directory or files that will be compiled.
     * Useful for debug, and for integration with Editor/IDE to reset markers only
     * for compiled files.
     *
     */
    @Parameter(property = "notifyCompilation", defaultValue = "true")
    private boolean notifyCompilation = true;

    abstract protected File getOutputDir() throws Exception;

    abstract protected List<String> getClasspathElements() throws Exception;

    private long _lastCompileAt = -1;

    private SbtIncrementalCompiler incremental;

    /**
     * Analysis cache file for incremental recompilation.
     */
    abstract protected File getAnalysisCacheFile() throws Exception;

    /**
     * Compile order for Scala and Java sources for sbt incremental compile.
     *
     * Can be Mixed, JavaThenScala, or ScalaThenJava.
     *
     */
    @Parameter(property = "compileOrder", defaultValue = "mixed")
    private String compileOrder;

    /**
     * Use zinc server for incremental recompilation.
     */
    @Parameter(property = "useZincServer", defaultValue = "false")
    private boolean useZincServer;

    /**
     * Zinc server port, if running with incremental zinc server mode.
     */
    @Parameter(property = "zincPort", defaultValue = "3030")
    private int zincPort;

    /**
     * Zinc server host, if running with incremental zinc server mode. Defaults to 127.0.0.1.
     */
    @Parameter(property = "zincHost", defaultValue = "127.0.0.1")
    private String zincHost;

    /**
     * Additional parameter to use to call zinc server
     * It is a pipe '|' separated list of arguments, so it can be used from command
     * line ("-DaddZincArgs=arg1|arg2|arg3|...").
     *
     */
    @Parameter(property = "addZincArgs")
    private String addZincArgs = "";

    @Override
    protected void doExecute() throws Exception {
        if (getLog().isDebugEnabled()) {
            for(File directory : getSourceDirectories()) {
                getLog().debug(FileUtils.pathOf(directory, useCanonicalPath));
            }
        }
        File outputDir = FileUtils.fileOf(getOutputDir(), useCanonicalPath);
        File analysisCacheFile = FileUtils.fileOf(getAnalysisCacheFile(), useCanonicalPath);
        int nbFiles = compile(getSourceDirectories(), outputDir, analysisCacheFile, getClasspathElements(), false);
        switch (nbFiles) {
            case -1:
                getLog().info("No sources to compile");
                break;
            case 0:
                getLog().info("Nothing to compile - all classes are up to date");;
                break;
            default:
                break;
        }
    }

    protected int compile(List<File> sourceRootDirs, File outputDir, File analysisCacheFile, List<String> classpathElements, boolean compileInLoop) throws Exception, InterruptedException {
        if (!compileInLoop && INCREMENTAL.equals(recompileMode)) {
            // TODO - Do we really need this dupliated here?
            if (!outputDir.exists()) {
              outputDir.mkdirs();
            }
            // if not compileInLoop, invoke incrementalCompile immediately
            return incrementalCompile(classpathElements, sourceRootDirs, outputDir, analysisCacheFile, compileInLoop);
        }

        long t0 = System.currentTimeMillis();
        LastCompilationInfo lastCompilationInfo = LastCompilationInfo.find(sourceRootDirs, outputDir);
        if (_lastCompileAt < 0) {
            _lastCompileAt = lastCompilationInfo.getLastSuccessfullTS();
        }

        List<File> files = getFilesToCompile(sourceRootDirs, _lastCompileAt);

        if (files == null) {
            return -1;
        }

        if (files.size() < 1) {
            return 0;
        }
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        long t1 = System.currentTimeMillis();

        if (compileInLoop && INCREMENTAL.equals(recompileMode)) {
            // if compileInLoop, do not invoke incrementalCompile when there's no change
            int retCode = incrementalCompile(classpathElements, sourceRootDirs, outputDir, analysisCacheFile, compileInLoop);
            _lastCompileAt = t1;
            if (retCode == 1) {
                lastCompilationInfo.setLastSuccessfullTS(t1);
            }
            return retCode;
        }

        getLog().info(String.format("Compiling %d source files to %s at %d", files.size(), outputDir.getAbsolutePath(), t1));
        JavaMainCaller jcmd = getScalaCommand();
        jcmd.redirectToLog();
        if (!classpathElements.isEmpty()) jcmd.addArgs("-classpath", MainHelper.toMultiPath(classpathElements));
        jcmd.addArgs("-d", outputDir.getAbsolutePath());
        //jcmd.addArgs("-sourcepath", sourceDir.getAbsolutePath());
        for (File f : files) {
            jcmd.addArgs(f.getAbsolutePath());
        }
        if (jcmd.run(displayCmd, !compileInLoop)) {
          lastCompilationInfo.setLastSuccessfullTS(t1);
        }
        else {
            compileErrors = true;
        }
        getLog().info(String.format("prepare-compile in %d s", (t1 - t0) / 1000));
        getLog().info(String.format("compile in %d s", (System.currentTimeMillis() - t1) / 1000));
        _lastCompileAt = t1;
        return files.size();
    }

    /**
     * Returns true if the previous compile failed
     */
    protected boolean hasCompileErrors() {
        return compileErrors;
    }

    protected void clearCompileErrors() {
        compileErrors = false;
    }

    protected List<File> getFilesToCompile(List<File> sourceRootDirs, long lastSuccessfullCompileTime) throws Exception {
        List<File> sourceFiles = findSourceWithFilters(sourceRootDirs);
        if (sourceFiles.size() == 0) {
            return null;
        }

        // filter uptodate
        // filter is not applied to .java, because scalac failed to used existing .class for unmodified .java
        //   failed with "error while loading Xxx, class file '.../target/classes/.../Xxxx.class' is broken"
        //   (restore how it work in 2.11 and failed in 2.12)
        //TODO a better behavior : if there is at least one .scala to compile then add all .java, if there is at least one .java then add all .scala (because we don't manage class dependency)
        List<File> files = new ArrayList<File>(sourceFiles.size());
        if (_lastCompileAt > 0 || (!ALL.equals(recompileMode) && (lastSuccessfullCompileTime > 0))) {
            ArrayList<File> modifiedScalaFiles = new ArrayList<File>(sourceFiles.size());
            ArrayList<File> modifiedJavaFiles = new ArrayList<File>(sourceFiles.size());
            ArrayList<File> allJavaFiles = new ArrayList<File>(sourceFiles.size());
            for (File f : sourceFiles) {
                if (f.getName().endsWith(".java")) {
                    allJavaFiles.add(f);
                }
                if (f.lastModified() >= lastSuccessfullCompileTime) {
                    if (f.getName().endsWith(".java")) {
                        modifiedJavaFiles.add(f);
                    } else {
                        modifiedScalaFiles.add(f);
                    }
                }
            }
            if ((modifiedScalaFiles.size() != 0) || (modifiedJavaFiles.size() != 0)) {
                if ((modifiedScalaFiles.size() != 0) && MODIFIED_ONLY.equals(recompileMode)) {
                    files.addAll(allJavaFiles);
                    files.addAll(modifiedScalaFiles);
                    notifyCompilation(files);
                } else {
                    files.addAll(sourceFiles);
                    notifyCompilation(sourceRootDirs);
                }
            }
        } else {
            files.addAll(sourceFiles);
            notifyCompilation(sourceRootDirs);
        }
        return files;
    }

    private void notifyCompilation(List<File> files) throws Exception {
        if (notifyCompilation) {
            for (File f : files) {
                getLog().info(String.format("%s:-1: info: compiling", FileUtils.pathOf(f, useCanonicalPath)));
            }
        }
    }

    private static class LastCompilationInfo {
      static LastCompilationInfo find(List<File> sourceRootDirs, File outputDir) throws Exception {
        StringBuilder hash = new StringBuilder();
        for (File f : sourceRootDirs) {
          hash.append(f.toString());
        }
        return new LastCompilationInfo(new File(outputDir.getAbsolutePath() + "." + hash.toString().hashCode() + ".timestamp"), outputDir);
      }

      private final File _lastCompileAtFile;
      private final File _outputDir;

      private LastCompilationInfo(File f, File outputDir) {
        _lastCompileAtFile = f;
        _outputDir = outputDir;
      }

      long getLastSuccessfullTS() throws Exception {
        long back =  -1;
        if (_lastCompileAtFile.exists() && _outputDir.exists() && (_outputDir.list().length > 0)) {
            back = _lastCompileAtFile.lastModified();
        }
        return back;
      }

      void setLastSuccessfullTS(long v) throws Exception {
        if (!_lastCompileAtFile.exists()) {
            FileUtils.fileWrite(_lastCompileAtFile.getAbsolutePath(), ".");
        }
        _lastCompileAtFile.setLastModified(v);
      }
    }

    //
    // Incremental compilation
    //

    @SuppressWarnings("unchecked")
    protected int incrementalCompile(List<String> classpathElements, List<File> sourceRootDirs, File outputDir, File cacheFile, boolean compileInLoop) throws Exception, InterruptedException {
        List<File> sources = findSourceWithFilters(sourceRootDirs);
        if (sources.isEmpty()) {
            return -1;
        }

        if (incremental == null) {
            File libraryJar = getLibraryJar();
            File compilerJar = getCompilerJar();
            List<File> extraJars = getCompilerDependencies();
            extraJars.remove(libraryJar);
            String sbtGroupId = SbtIncrementalCompiler.SBT_GROUP_ID;
            String xsbtiArtifactId = SbtIncrementalCompiler.XSBTI_ARTIFACT_ID;
            String compilerInterfaceArtifactId = SbtIncrementalCompiler.COMPILER_INTERFACE_ARTIFACT_ID;
            String compilerInterfaceClassifier = SbtIncrementalCompiler.COMPILER_INTERFACE_CLASSIFIER;
            String sbtVersion = findVersionFromPluginArtifacts(sbtGroupId, SbtIncrementalCompiler.COMPILER_INTEGRATION_ARTIFACT_ID);
            File xsbtiJar = getPluginArtifactJar(sbtGroupId, xsbtiArtifactId, sbtVersion);
            List<String> zincArgs = StringUtils.isEmpty(addZincArgs) ? new LinkedList<String>() : (List<String>) Arrays.asList(StringUtils.split(addZincArgs, "|"));
            File interfaceSrcJar = getPluginArtifactJar(sbtGroupId, compilerInterfaceArtifactId, sbtVersion, compilerInterfaceClassifier);
           	incremental = new SbtIncrementalCompiler(useZincServer, zincHost, zincPort, libraryJar, compilerJar, extraJars, xsbtiJar, interfaceSrcJar, getLog(), zincArgs);
        }

        classpathElements.remove(outputDir.getAbsolutePath());
        List<String> scalacOptions = getScalaOptions();
        List<String> javacOptions = getJavacOptions();
        Map<File, File> cacheMap = getAnalysisCacheMap();

        try {
            incremental.compile(project.getBasedir(), classpathElements, sources, outputDir, scalacOptions, javacOptions, cacheFile, cacheMap, compileOrder, toolchainManager.getToolchainFromBuildContext("jdk", session));
        } catch (xsbti.CompileFailed e) {
            if (compileInLoop) {
                compileErrors = true;
            } else {
                throw e;
            }
        }

        return 1;
    }

    protected Map<File, File> getAnalysisCacheMap() {
        HashMap<File, File> map = new HashMap<File, File>();
        String scalaPluginKey = ((PluginDescriptor) getPluginContext().get("pluginDescriptor")).getPluginLookupKey();
        for (MavenProject project1 : reactorProjects) {
            Plugin plugin = project1.getPlugin(scalaPluginKey);
            if (plugin != null) {
                Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();
                Xpp3Dom analysisCache = (configuration != null) ? configuration.getChild("analysisCacheFile") : null;
                File analysisCacheFile = (analysisCache != null) ? new File(analysisCache.getValue()) : defaultAnalysisCacheFile(project1);
                File classesDirectory = new File(project1.getBuild().getOutputDirectory());
                map.put(classesDirectory.getAbsoluteFile(), analysisCacheFile.getAbsoluteFile());
                Xpp3Dom testAnalysisCache = (configuration != null) ? configuration.getChild("testAnalysisCacheFile") : null;
                File testAnalysisCacheFile = (testAnalysisCache != null) ? new File(testAnalysisCache.getValue()) : defaultTestAnalysisCacheFile(project1);
                File testClassesDirectory = new File(project1.getBuild().getTestOutputDirectory());
                map.put(testClassesDirectory.getAbsoluteFile(), testAnalysisCacheFile.getAbsoluteFile());
            }
        }
        return map;
    }

    protected File defaultAnalysisDirectory(MavenProject p) {
        return new File(p.getBuild().getDirectory(), "analysis");
    }

    protected File defaultAnalysisCacheFile(MavenProject p) {
        return new File(defaultAnalysisDirectory(p), "compile");
    }

    protected File defaultTestAnalysisCacheFile(MavenProject p) {
        return new File(defaultAnalysisDirectory(p), "test-compile");
    }
}

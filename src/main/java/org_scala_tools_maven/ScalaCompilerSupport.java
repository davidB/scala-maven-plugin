/*
 * Copyright 2007 scala-tools.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org_scala_tools_maven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.codehaus.plexus.util.FileUtils;
import org_scala_tools_maven_executions.JavaMainCaller;
import org_scala_tools_maven_executions.MainHelper;

/**
 * Abstract parent of all Scala Mojo who run compilation
 */
public abstract class ScalaCompilerSupport extends ScalaMojoSupport {
    public static final String ALL = "all";
    public static final String MODIFIED_ONLY = "modified-only";

    /**
     * Pause duration between to scan to detect changed file to compile.
     * Used only if compileInLoop or testCompileInLoop is true.
     */
    protected long loopSleep = 2500;

    /**
     * compilation-mode to use when sources was previously compiled and there is at least one change:
     * "modified-only" => only modified source was recompiled (pre 2.13 behavior), "all" => every source are recompiled
     * @parameter expression="${recompilation-mode}" default-value="all"
     */
    private String recompileMode = ALL;

    /**
     * notifyCompilation if true then print a message "path: compiling"
     * for each root directory or files that will be compiled.
     * Usefull for debug, and for integration with Editor/IDE to reset markers only for compiled files.
     *
     * @parameter expression="${notifyCompilation}" default-value="true"
     */
    private boolean notifyCompilation = true;

    /**
     * Enables/Disables sending java source to the scala compiler.
     *
     * @parameter default-value="true"
     */
    protected boolean sendJavaToScalac = true;

    /**
     * A list of inclusion filters for the compiler.
     * ex :
     * <pre>
     *    &lt;includes&gt;
     *      &lt;include&gt;SomeFile.scala&lt;/include&gt;
     *    &lt;/includes&gt;
     * </pre>
     *
     * @parameter
     */
    private Set<String> includes = new HashSet<String>();

    /**
     * A list of exclusion filters for the compiler.
     * ex :
     * <pre>
     *    &lt;excludes&gt;
     *      &lt;exclude&gt;SomeBadFile.scala&lt;/exclude&gt;
     *    &lt;/excludes&gt;
     * </pre>
     *
     * @parameter
     */
    private Set<String> excludes = new HashSet<String>();

    abstract protected File getOutputDir() throws Exception;

    abstract protected List<String> getClasspathElements() throws Exception;

    /**
     * Retreives the list of *all* root source directories.  We need to pass all .java and .scala files into the scala compiler
     */
    abstract protected List<String> getSourceDirectories() throws Exception;

    private long _lastCompileAt = -1;

    @Override
    protected void doExecute() throws Exception {
        File outputDir = normalize(getOutputDir());
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        if (getLog().isDebugEnabled()) {
            for(String directory : getSourceDirectories()) {
                getLog().debug(directory);
            }
        }
        int nbFiles = compile(normalize(getSourceDirectories()), outputDir, getClasspathElements(), false);
        switch (nbFiles) {
            case -1:
                getLog().warn("No source files found.");
                break;
            case 0:
                getLog().info("Nothing to compile - all classes are up to date");;
                break;
            default:
                break;
        }
    }

    protected File normalize(File f) {
        try {
            f = f.getCanonicalFile();
        } catch (IOException exc) {
            f = f.getAbsoluteFile();
        }
        return f;
    }

    /**
     * This limits the source directories to only those that exist for real.
     */
    private List<File> normalize(List<String> compileSourceRootsList) {
        List<File> newCompileSourceRootsList = new ArrayList<File>();
        if (compileSourceRootsList != null) {
            // copy as I may be modifying it
            for (String srcDir : compileSourceRootsList) {
                File srcDirFile = normalize(new File(srcDir));
                if (!newCompileSourceRootsList.contains(srcDirFile) && srcDirFile.exists()) {
                    newCompileSourceRootsList.add(srcDirFile);
                }
            }
        }
        return newCompileSourceRootsList;
    }

    protected int compile(File sourceDir, File outputDir, List<String> classpathElements, boolean compileInLoop) throws Exception, InterruptedException {
        //getLog().warn("Using older form of compile");
        return compile(Arrays.asList(sourceDir), outputDir, classpathElements, compileInLoop);
    }

    protected int compile(List<File> sourceRootDirs, File outputDir, List<String> classpathElements, boolean compileInLoop) throws Exception, InterruptedException {
        long t0 = System.currentTimeMillis();
        if (_lastCompileAt < 0) {
            _lastCompileAt = findLastSuccessfullCompilation(outputDir);
        }

        List<File> files = getFilesToCompile(sourceRootDirs, _lastCompileAt);

        if (files == null) {
            return -1;
        }

        if (files.size() < 1) {
            return 0;
        }
        long t1 = System.currentTimeMillis();
        getLog().info(String.format("Compiling %d source files to %s at %d", files.size(), outputDir.getAbsolutePath(), t1));
        JavaMainCaller jcmd = getScalaCommand();
        jcmd.redirectToLog();
        jcmd.addArgs("-classpath", MainHelper.toMultiPath(classpathElements));
        jcmd.addArgs("-d", outputDir.getAbsolutePath());
        //jcmd.addArgs("-sourcepath", sourceDir.getAbsolutePath());
        for (File f : files) {
            jcmd.addArgs(f.getAbsolutePath());
        }
        if (jcmd.run(displayCmd, !compileInLoop)) {
            setLastSuccessfullCompilation(outputDir, t1);
        }
        getLog().info(String.format("prepare-compile in %d s", (t1 - t0) / 1000));
        getLog().info(String.format("compile in %d s", (System.currentTimeMillis() - t1) / 1000));
        _lastCompileAt = t1;
        return files.size();
     }

    protected List<File> getFilesToCompile(List<File> sourceRootDirs, long lastSuccessfullCompileTime) throws Exception {
        // TODO - Rather than mutate, pass to the function!
        if (includes.isEmpty()) {
            includes.add("**/*.scala");
            if (sendJavaToScalac && isJavaSupportedByCompiler()) {
                includes.add("**/*.java");
            }
        }

        if ((_lastCompileAt <0) && getLog().isInfoEnabled()) {
            StringBuilder builder = new StringBuilder("includes = [");
            for (String include : includes) {
                builder.append(include).append(",");
            }
            builder.append("]");
            getLog().info(builder.toString());

            builder = new StringBuilder("excludes = [");
            for (String exclude : excludes) {
                builder.append(exclude).append(",");
            }
            builder.append("]");
            getLog().info(builder.toString());
        }

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

    /**
     * Finds all source files in a set of directories with a given extension.
     */
    private List<File> findSourceWithFilters(List<File> sourceRootDirs) {
        List<File> sourceFiles = new ArrayList<File>();
        // TODO - Since we're making files anyway, perhaps we should just test
        // for existence here...
        for (File dir : sourceRootDirs) {
            String[] tmpFiles = MainHelper.findFiles(dir, includes.toArray(new String[includes.size()]), excludes.toArray(new String[excludes.size()]));
            for (String tmpLocalFile : tmpFiles) {
                File tmpAbsFile = normalize(new File(dir, tmpLocalFile));
                sourceFiles.add(tmpAbsFile);
            }
        }
        //scalac is sensible to scala file order, file system can't garanty file order => unreproductible build error across platform
        // to garanty reproductible command line we order file by path (os dependend).
        Collections.sort(sourceFiles);
        return sourceFiles;
    }

    private void notifyCompilation(List<File> files) throws Exception {
        if (notifyCompilation) {
            for (File f : files) {
                getLog().info(String.format("%s:-1: info: compiling", f.getCanonicalPath()));
            }
        }
    }

    private long findLastSuccessfullCompilation(File outputDir) throws Exception {
        long back =  -1;
        final File lastCompileAtFile = new File(outputDir + ".timestamp");
        if (lastCompileAtFile.exists() && outputDir.exists() && (outputDir.list().length > 0)) {
            back = lastCompileAtFile.lastModified();
        }
        return back;
    }

    private void setLastSuccessfullCompilation(File outputDir, long v) throws Exception {
        final File lastCompileAtFile = new File(outputDir + ".timestamp");
        if (lastCompileAtFile.exists()) {
        } else {
            FileUtils.fileWrite(lastCompileAtFile.getAbsolutePath(), ".");
        }
        lastCompileAtFile.setLastModified(v);
    }
}
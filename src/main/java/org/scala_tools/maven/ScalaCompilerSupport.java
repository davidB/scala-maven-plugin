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
package org.scala_tools.maven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.codehaus.plexus.util.FileUtils;
import org.scala_tools.maven.executions.JavaMainCaller;
import org.scala_tools.maven.executions.MainHelper;

/**
 * Abstract parent of all Scala Mojo
 */
public abstract class ScalaCompilerSupport extends ScalaMojoSupport {
    /**
     * Pause duration between to scan to detect changed file to compile.
     * Used only if compileInLoop or testCompileInLoop is true.
     */
    protected long loopSleep = 2500;

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
        int nbFiles = compile(getSourceDirectories(), outputDir, getClasspathElements(), false);
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


    protected int compile(File sourceDir, File outputDir, List<String> classpathElements, boolean compileInLoop) throws Exception, InterruptedException {
        //getLog().warn("Using older form of compile");
        return compile(Arrays.asList(sourceDir.getAbsolutePath()), outputDir, classpathElements, compileInLoop);
    }

    protected int compile(List<String> sourceRootDirs, File outputDir, List<String> classpathElements, boolean compileInLoop) throws Exception, InterruptedException {
        final File lastCompileAtFile = new File(outputDir + ".timestamp");
        long lastCompileAt = -1;
        if (lastCompileAtFile.exists() && outputDir.exists() && (outputDir.list().length > 0)) {
            lastCompileAt = lastCompileAtFile.lastModified();
        }

        List<File> files = getFilesToCompile(sourceRootDirs, compileInLoop, lastCompileAt);

        if (files == null) {
            return -1;
        }

        if (!compileInLoop) {
            getLog().info(String.format("Compiling %d source files to %s", files.size(), outputDir.getAbsolutePath()));
        }
        if (files.size() < 1) {
            return 0;
        }
        long now = System.currentTimeMillis();
        JavaMainCaller jcmd = getScalaCommand();
        jcmd.addArgs("-classpath", MainHelper.toMultiPath(classpathElements));
        jcmd.addArgs("-d", outputDir.getAbsolutePath());
        //jcmd.addArgs("-sourcepath", sourceDir.getAbsolutePath());
        for (File f : files) {
            jcmd.addArgs(f.getAbsolutePath());
            if (compileInLoop) {
                getLog().info(String.format("%tR compiling %s", now, f.getName()));
            }
        }
        jcmd.run(displayCmd, !compileInLoop);
        if (lastCompileAtFile.exists()) {
            lastCompileAtFile.setLastModified(now);
        } else {
            FileUtils.fileWrite(lastCompileAtFile.getAbsolutePath(), ".");
        }
        return files.size();
     }

    protected List<File> getFilesToCompile(List<String> sourceRootDirs, boolean compilingInLoop, long lastCompileTime) {
        // TODO - Rather than mutate, pass to the function!
        if (includes.isEmpty()) {
            includes.add("**/*.scala");
            if (sendJavaToScalac && !compilingInLoop && isJavaSupportedByCompiler()) {
                includes.add("**/*.java");
            }
        }

        if (getLog().isInfoEnabled()) {
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

        List<String> scalaSourceFiles = findSourceWithFilters(sourceRootDirs);
        if (scalaSourceFiles.size() == 0) {
            return null;
        }

        // filter uptodate
        ArrayList<File> files = new ArrayList<File>(scalaSourceFiles.size());
        for (String x : scalaSourceFiles) {
            File f = new File(x);
            if (f.lastModified() >= lastCompileTime) {
                files.add(f);
            }
        }
        return files;
    }

    /**
     * Finds all source files in a set of directories with a given extension.
     */
    private List<String> findSourceWithFilters(List<String> sourceRootDirs) {
        List<String> sourceFiles = new ArrayList<String>();
        // TODO - Since we're making files anyway, perhaps we should just test
        // for existence here...
        for (String rootSourceDir : normalizeSourceRoots(sourceRootDirs)) {
            File dir = normalize(new File(rootSourceDir));
            String[] tmpFiles = MainHelper.findFiles(dir, includes.toArray(new String[includes.size()]), excludes.toArray(new String[excludes.size()]));
            for (String tmpLocalFile : tmpFiles) {
                File tmpAbsFile = normalize(new File(dir, tmpLocalFile));
                sourceFiles.add(tmpAbsFile.getAbsolutePath());
            }
        }
        return sourceFiles;
    }

    /**
     * This limits the source directories to only those that exist for real.
     */
    private List<String> normalizeSourceRoots(List<String> compileSourceRootsList) {
        List<String> newCompileSourceRootsList = new ArrayList<String>();
        if (compileSourceRootsList != null) {
            // copy as I may be modifying it
            for (String srcDir : compileSourceRootsList) {
                File srcDirFile = normalize(new File(srcDir));
                if (!newCompileSourceRootsList.contains(srcDirFile.getAbsolutePath()) && srcDirFile.exists()) {
                    newCompileSourceRootsList.add(srcDirFile.getAbsolutePath());
                }
            }
        }
        return newCompileSourceRootsList;
    }
}

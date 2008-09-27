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
import java.util.List;

import org.codehaus.plexus.util.FileUtils;

/**
 * Abstract parent of all Scala Mojo
 */
public abstract class ScalaCompilerSupport extends ScalaMojoSupport {
    /**
     * Pause duration between to scan to detect changed file to compile.
     * Used only if compileInLoop or testCompileInLoop is true.
     */
    protected long loopSleep = 2500;


    abstract protected File getOutputDir() throws Exception;

    abstract protected File getSourceDir() throws Exception;

    abstract protected List<String> getClasspathElements() throws Exception;

    @Override
    protected void doExecute() throws Exception {
        File outputDir = normalize(getOutputDir());
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        File sourceDir = normalize(getSourceDir());
        if (!sourceDir.exists()) {
            return;
        }
        int nbFiles = compile(sourceDir, outputDir, getClasspathElements(), false);
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
        String[] sourceFiles = JavaCommand.findFiles(sourceDir, "**/*.scala");
        if (sourceFiles.length == 0) {
            return -1;
        }
        int sourceDirPathLength = sourceDir.getAbsolutePath().length();

        // filter uptodate
        File lastCompileAtFile = new File(outputDir + ".timestamp");
        long lastCompileAt = lastCompileAtFile.lastModified();
        ArrayList<File> files = new ArrayList<File>(sourceFiles.length);
        for (String x : sourceFiles) {
            File f = new File(sourceDir, x);
            if (f.lastModified() >= lastCompileAt) {
                files.add(f);
            }
        }
        if (files.size() == 0) {
            return 0;
        }
        if (!compileInLoop) {
            getLog().info(String.format("Compiling %d source files to %s", files.size(), outputDir.getAbsolutePath()));
        }
        long now = System.currentTimeMillis();
        JavaCommand jcmd = getScalaCommand();
        jcmd.addArgs("-classpath", JavaCommand.toMultiPath(classpathElements));
        jcmd.addArgs("-d", outputDir.getAbsolutePath());
        jcmd.addArgs("-sourcepath", sourceDir.getAbsolutePath());
        for (File f : files) {
            jcmd.addArgs(f.getAbsolutePath());
            if (compileInLoop) {
                getLog().info(String.format("%tR compiling %s", now, f.getAbsolutePath().substring(sourceDirPathLength+1)));
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
}

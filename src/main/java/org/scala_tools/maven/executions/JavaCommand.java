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
package org.scala_tools.maven.executions;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.StringUtils;

/**
 * Helper class use to call a java Main in an external process.
 */
public class JavaCommand extends AbstractForkedJavaCommand {
    // //////////////////////////////////////////////////////////////////////////
    // Class
    // //////////////////////////////////////////////////////////////////////////
    public static String toMultiPath(List<String> paths) {
        return StringUtils.join(paths.iterator(), File.pathSeparator);
    }

    public static String toMultiPath(String[] paths) {
        return StringUtils.join(paths, File.pathSeparator);
    }

    public static String[] findFiles(File dir, String pattern) {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(dir);
        scanner.setIncludes(new String[] { pattern });
        scanner.addDefaultExcludes();
        scanner.scan();
        return scanner.getIncludedFiles();
    }

    public static String toClasspathString(ClassLoader cl) throws Exception {
        if (cl == null) {
            cl = Thread.currentThread().getContextClassLoader();
        }
        StringBuilder back = new StringBuilder();
        while (cl != null) {
            if (cl instanceof URLClassLoader) {
                URLClassLoader ucl = (URLClassLoader) cl;
                URL[] urls = ucl.getURLs();
                for (URL url : urls) {
                    if (back.length() != 0) {
                        back.append(File.pathSeparatorChar);
                    }
                    back.append(url.getFile());
                }
            }
            cl = cl.getParent();
        }
        return back.toString();
    }

    // //////////////////////////////////////////////////////////////////////////
    // Object
    // //////////////////////////////////////////////////////////////////////////

    private boolean _forceUseArgFile = false;
    
    public JavaCommand(AbstractMojo requester, String mainClassName, String classpath, String[] jvmArgs, String[] args, boolean forceUseArgFile) throws Exception {
       super(requester, mainClassName, classpath, jvmArgs, args);
       _forceUseArgFile = forceUseArgFile;
    }

    protected String[] buildCommand() throws Exception {
        ArrayList<String> back = new ArrayList<String>(2 + jvmArgs.size() + args.size());
        back.add(javaExec);
        if (!_forceUseArgFile && (lengthOf(args, 1) + lengthOf(jvmArgs, 1) < 400)) {
            back.addAll(jvmArgs);
            back.add(mainClassName);
            back.addAll(args);
        } else {
            File jarPath = new File(MainHelper.locateJar(MainHelper.class));
            addToClasspath(jarPath);
            back.addAll(jvmArgs);
            back.add(MainWithArgsInFile.class.getName());
            back.add(mainClassName);
            back.add(MainHelper.createArgFile(args).getCanonicalPath());
        }
        return back.toArray(new String[back.size()]);
    }

    private long lengthOf(List<String> l, long sepLength) throws Exception {
        long back = 0;
        for(String str : l) {
            back += str.length() + sepLength;
        }
        return back;
    }
}

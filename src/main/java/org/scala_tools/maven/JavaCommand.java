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
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.StringUtils;

/**
 * Helper class use to call a java Main in an external process.
 */
public class JavaCommand {
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
    private AbstractMojo requester_;

    private List<String> env_;
    private List<String> jvmArgs_;
    private List<String> args_;
    private String javaExec_;
    private String mainClassName_;
    private boolean logOnly_ = true;

    public JavaCommand(AbstractMojo requester, String mainClassName, String classpath, String[] jvmArgs, String[] args) throws Exception {
        requester_ = requester;
        env_ = new ArrayList<String>();
        for (String key : System.getenv().keySet()) {
            env_.add(key + "=" + System.getenv(key));
        }
        javaExec_ = System.getProperty("java.home");
        if (javaExec_ == null) {
            javaExec_ = System.getenv("JAVA_HOME");
            if (javaExec_ == null) {
                throw new IllegalStateException("Couldn't locate java, try setting JAVA_HOME environment variable.");
            }
        }
        javaExec_ += File.separator + "bin" + File.separator + "java";
        mainClassName_ = mainClassName;
        jvmArgs_ = new ArrayList<String>();
        args_ = new ArrayList<String>();
        addJvmArgs("-classpath", classpath);
        addJvmArgs(jvmArgs);
        addArgs(args);
    }

    public void addEnvVar(String key, String value) {
        env_.add(key + "=" + value);
    }

    public void addJvmArgs(String... args) {
        if (args == null) {
            return;
        }
        for (String arg : args) {
            jvmArgs_.add(arg);
        }
    }

    public void addArgs(String... args) {
        if (args == null) {
            return;
        }
        for (String arg : args) {
            args_.add(arg);
        }
    }

    public void addOption(String key, String value) {
        if ((value == null) || (key == null)) {
            return;
        }
        args_.add(key);
        args_.add(value);
    }

    public void addOption(String key, File value) {
        if ((value == null) || (key == null)) {
            return;
        }
        args_.add(key);
        args_.add(value.getAbsolutePath());
    }

    public void addOption(String key, boolean value) {
        if ((!value) || (key == null)) {
            return;
        }
        args_.add(key);
    }

    public void setLogOnly(boolean v) {
        logOnly_ = v;
    }

    private String[] buildCommand() {
        ArrayList<String> back = new ArrayList<String>(2 + jvmArgs_.size() + args_.size());
        back.add(javaExec_);
        back.addAll(jvmArgs_);
        back.add(mainClassName_);
        back.addAll(args_);
        return back.toArray(new String[back.size()]);
    }

    // TODO: avoid to have several Thread to pipe stream
    // TODO: add support to inject startup command and shutdown command (on :quit)
    public void run(boolean displayCmd) throws Exception {
        run(displayCmd, true);
    }

    public void run(boolean displayCmd, boolean throwFailure) throws Exception {

        String[] cmd = buildCommand();
        if (displayCmd) {
            requester_.getLog().info("cmd: " + " " + StringUtils.join(cmd, " "));
        } else if (requester_.getLog().isDebugEnabled()) {
            requester_.getLog().debug("cmd: " + " " + StringUtils.join(cmd, " "));
        }
        ProcessBuilder pb = new ProcessBuilder(cmd);
        //pb.directory("myDir");
        if (!logOnly_) {
            pb = pb.redirectErrorStream(true);
        }
        Process p = pb.start();
        if (logOnly_) {
            new StreamLogger(p.getErrorStream(), requester_.getLog(), true).start();
            new StreamLogger(p.getInputStream(), requester_.getLog(), false).start();
        } else {
            new StreamPiper(p.getInputStream(), System.out).start();
            //new StreamPiper(System.in, p.getOutputStream()).start();
            new ConsolePiper(p).start();
        }
        int retVal = p.waitFor();
        if (throwFailure && (retVal != 0)) {
            throw new MojoFailureException("command line returned non-zero value:" + retVal);
        }
    }

    /**
     * run the command without stream redirection nor waiting for exit
     *
     * @param displayCmd
     * @throws Exception
     */
    public void spawn(boolean displayCmd) throws Exception {
        String[] cmd = buildCommand();
        if (displayCmd) {
            requester_.getLog().info("cmd: " + " " + StringUtils.join(cmd, " "));
        } else if (requester_.getLog().isDebugEnabled()) {
            requester_.getLog().debug("cmd: " + " " + StringUtils.join(cmd, " "));
        }
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.start();
    }

}

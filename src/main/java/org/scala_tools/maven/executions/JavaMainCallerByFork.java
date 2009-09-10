package org.scala_tools.maven.executions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.StringUtils;

/**
 * forked java commands.
 *
 * @author D. Bernard
 * @author J. Suereth
 */
public class JavaMainCallerByFork extends JavaMainCallerSupport {

    private boolean _forceUseArgFile = false;

    /**
     * Location of java executable.
     */
    private String _javaExec;

    public JavaMainCallerByFork(AbstractMojo requester, String mainClassName, String classpath, String[] jvmArgs, String[] args,
            boolean forceUseArgFile) throws Exception {
        super(requester, mainClassName, classpath, jvmArgs, args);
        for (String key : System.getenv().keySet()) {
            env.add(key + "=" + System.getenv(key));
        }
        _javaExec = System.getProperty("java.home");
        if (_javaExec == null) {
            _javaExec = System.getenv("JAVA_HOME");
            if (_javaExec == null) {
                throw new IllegalStateException("Couldn't locate java, try setting JAVA_HOME environment variable.");
            }
        }
        _javaExec += File.separator + "bin" + File.separator + "java";
        _forceUseArgFile = forceUseArgFile;
    }

    public void run(boolean displayCmd, boolean throwFailure) throws Exception {
        String[] cmd = buildCommand();
        if (displayCmd) {
            requester.getLog().info("cmd: " + " " + StringUtils.join(cmd, " "));
        } else if (requester.getLog().isDebugEnabled()) {
            requester.getLog().debug("cmd: " + " " + StringUtils.join(cmd, " "));
        }
        Executor exec = new DefaultExecutor();
        CommandLine cl = new CommandLine(cmd[0]);
        for (int i = 1; i < cmd.length; i++) {
            cl.addArgument(cmd[i]);
        }
        int exitValue = exec.execute(cl);

        if (throwFailure && (exitValue != 0)) {
            throw new MojoFailureException("command line returned non-zero value:" + exitValue);
        }
    }

    public void spawn(boolean displayCmd) throws Exception {
        String[] cmd = buildCommand();
        if (displayCmd) {
            requester.getLog().info("cmd: " + " " + StringUtils.join(cmd, " "));
        } else if (requester.getLog().isDebugEnabled()) {
            requester.getLog().debug("cmd: " + " " + StringUtils.join(cmd, " "));
        }
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.start();
    }

    protected String[] buildCommand() throws Exception {
        ArrayList<String> back = new ArrayList<String>(2 + jvmArgs.size() + args.size());
        back.add(_javaExec);
        if (!_forceUseArgFile && (lengthOf(args, 1) + lengthOf(jvmArgs, 1) < 400)) {
            back.addAll(jvmArgs);
            back.add(mainClassName);
            back.addAll(args);
        } else {
            File jarPath = new File(MainHelper.locateJar(MainHelper.class));
            requester.getLog().debug("plugin jar to add :" + jarPath);
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
        for (String str : l) {
            back += str.length() + sepLength;
        }
        return back;
    }
}
package scala_maven_executions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.exec.OS;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.toolchain.Toolchain;
import org.codehaus.plexus.util.StringUtils;

import scala_maven_executions.LogProcessorUtils.LevelState;
import util.JavaLocator;

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

    private boolean _redirectToLog;

    public JavaMainCallerByFork(AbstractMojo requester1, String mainClassName1, String classpath, String[] jvmArgs1, String[] args1, boolean forceUseArgFile, Toolchain toolchain) throws Exception {
        super(requester1, mainClassName1, classpath, jvmArgs1, args1);
        for (String key : System.getenv().keySet()) {
            env.add(key + "=" + System.getenv(key));
        }

        _javaExec = JavaLocator.findExecutableFromToolchain(toolchain);
        _forceUseArgFile = forceUseArgFile;
    }

    @Override
    public boolean run(boolean displayCmd, boolean throwFailure) throws Exception {
        List<String> cmd = buildCommand();
        displayCmd(displayCmd, cmd);
        Executor exec = new DefaultExecutor();

        //err and out are redirected to out
        if (!_redirectToLog) {
            exec.setStreamHandler(new PumpStreamHandler(System.out, System.err, System.in));
        } else {
            exec.setStreamHandler(new PumpStreamHandler(new LogOutputStream() {
                private LevelState _previous = new LevelState();

                @Override
                protected void processLine(String line, int level) {
                    try {
                        _previous = LogProcessorUtils.levelStateOf(line, _previous);
                        switch (_previous.level) {
                        case ERROR:
                            requester.getLog().error(line);
                            break;
                        case WARNING:
                            requester.getLog().warn(line);
                            break;
                        default:
                            requester.getLog().info(line);
                            break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }));
        }

        CommandLine cl = new CommandLine(cmd.get(0));
        for (int i = 1; i < cmd.size(); i++) {
            cl.addArgument(cmd.get(i), false);
        }
        try {
            int exitValue = exec.execute(cl);
            if (exitValue != 0) {
                if (throwFailure) {
                    throw new MojoFailureException("command line returned non-zero value:" + exitValue);
                }
                return false;
            }
            if (!displayCmd) tryDeleteArgFile(cmd);
            return true;
        } catch (ExecuteException exc) {
            if (throwFailure) {
                throw exc;
            }
            return false;
        }
    }

    @Override
    public SpawnMonitor spawn(boolean displayCmd) throws Exception {
        List<String> cmd = buildCommand();
        File out = new File(System.getProperty("java.io.tmpdir"), mainClassName +".out");
        out.delete();
        cmd.add(">"+ out.getCanonicalPath());
        File err = new File(System.getProperty("java.io.tmpdir"), mainClassName +".err");
        err.delete();
        cmd.add("2>"+ err.getCanonicalPath());
        List<String> cmd2 = new ArrayList<String>();
        if (OS.isFamilyDOS()) {
            cmd2.add("cmd.exe");
            cmd2.add("/C");
            cmd2.addAll(cmd);
        } else {
            cmd2.add("/bin/sh");
            cmd2.add("-c");
            cmd2.addAll(cmd);
        }
        displayCmd(displayCmd, cmd2);
        ProcessBuilder pb = new ProcessBuilder(cmd2);
        //pb.redirectErrorStream(true);
        final Process p = pb.start();
        return new SpawnMonitor(){
            @Override
            public boolean isRunning() throws Exception {
                try {
                    p.exitValue();
                    return false;
                } catch(IllegalThreadStateException e) {
                    return true;
                }
            }
        };
    }

    private void displayCmd(boolean displayCmd, List<String> cmd) {
        if (displayCmd) {
            requester.getLog().info("cmd: " + " " + StringUtils.join(cmd.iterator(), " "));
        } else if (requester.getLog().isDebugEnabled()) {
            requester.getLog().debug("cmd: " + " " + StringUtils.join(cmd.iterator(), " "));
        }
    }

    protected List<String> buildCommand() throws Exception {
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
        return back;
    }

    private void tryDeleteArgFile(List<String> cmd) throws Exception {
    	String last = cmd.get(cmd.size() - 1);
    	if (last.endsWith(MainHelper.argFileSuffix)) {
    		File f = new File(last);
    		if (f.exists() && f.getName().startsWith(MainHelper.argFilePrefix)) {
    			f.delete();
    		}
    	}
    }
    private long lengthOf(List<String> l, long sepLength) throws Exception {
        long back = 0;
        for (String str : l) {
            back += str.length() + sepLength;
        }
        return back;
    }

    @Override
    public void redirectToLog() {
        _redirectToLog = true;
    }

}

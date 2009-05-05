package org.scala_tools.maven.executions;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
/**
 * Abstract helper implementation for JavaMainCaller interface.
 * @author josh
 *
 */
public abstract class AbstractJavaMainCaller implements JavaMainCaller {

    protected boolean logOnly = false;
    protected AbstractMojo requester;
    protected List<String> env  = new ArrayList<String>();
    protected String mainClassName;
    protected List<String> jvmArgs = new ArrayList<String>();
    protected List<String> args = new ArrayList<String>();


    protected AbstractJavaMainCaller(AbstractMojo requester, String mainClassName, String classpath, String[] jvmArgs, String[] args) throws Exception {
        this.requester = requester;
        for (String key : System.getenv().keySet()) {
            env.add(key + "=" + System.getenv(key));
        }
        this.mainClassName = mainClassName;
        addJvmArgs("-classpath", classpath);
        addJvmArgs(jvmArgs);
        addArgs(args);
    }
	
    public void addJvmArgs(String... args) {
        if(args != null) {
            for(String arg : args) {
                this.jvmArgs.add(arg);
            }
        }
    }

    public void addOption(String key, String value) {
        if ((value == null) || (key == null)) {
            return;
        }
        addArgs(key, value);
    }

    public void addOption(String key, File value) {
        if ((value == null) || (key == null)) {
            return;
        }
        addArgs(key, value.getAbsolutePath());
    }

    public void addOption(String key, boolean value) {
        if ((!value) || (key == null)) {
            return;
        }
        addArgs(key);
    }
    public void addArgs(String... args) {
        if(args != null) {
            this.args.addAll(Arrays.asList(args));
        }
    }

    public void addEnvVar(String key, String value) {
        this.env.add(key + "=" + value);

    }
    public void run(boolean displayCmd) throws Exception {
        run(displayCmd, true);
    }

    public void setLogOnly(boolean v) {
        logOnly = v;
    }
    public boolean getLogOnly() {
        return logOnly;
    }

}

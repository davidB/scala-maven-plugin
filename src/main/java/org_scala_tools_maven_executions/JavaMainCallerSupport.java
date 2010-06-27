package org_scala_tools_maven_executions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.codehaus.plexus.util.StringUtils;
/**
 * Abstract helper implementation for JavaMainCaller interface.
 * @author josh
 *
 */
public abstract class JavaMainCallerSupport implements JavaMainCaller {

    protected AbstractMojo requester;
    protected List<String> env  = new ArrayList<String>();
    protected String mainClassName;
    protected List<String> jvmArgs = new ArrayList<String>();
    protected List<String> args = new ArrayList<String>();


    protected JavaMainCallerSupport(AbstractMojo requester, String mainClassName, String classpath, String[] jvmArgs, String[] args) throws Exception {
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
                if (StringUtils.isNotEmpty(arg)) {
                    this.jvmArgs.add(arg);
                }
            }
        }
    }

    public void addToClasspath(File entry) throws Exception {
        if ((entry == null) || !entry.exists()) {
            return;
        }
        boolean isClasspath = false;
        for (int i = 0; i < jvmArgs.size(); i++) {
            String item = jvmArgs.get(i);
            if (isClasspath) {
                item = item + File.pathSeparator + entry.getCanonicalPath();
                jvmArgs.set(i, item);
                isClasspath = false;
                break;
            }
            isClasspath = "-classpath".equals(item);
        }
    }

    public void addOption(String key, String value) {
        if (StringUtils.isEmpty(value) || StringUtils.isEmpty(key)) {
            return;
        }
        addArgs(key, value);
    }

    public void addOption(String key, File value) {
        if ( (value == null) || StringUtils.isEmpty(key)) {
            return;
        }
        addArgs(key, value.getAbsolutePath());
    }

    public void addOption(String key, boolean value) {
        if ((!value) || StringUtils.isEmpty(key)) {
            return;
        }
        addArgs(key);
    }
    public void addArgs(String... args) {
        if(args != null) {
            for(String arg : args) {
                if (StringUtils.isNotEmpty(arg)) {
                    this.args.add(arg);
                }
            }
        }
    }

    public void addEnvVar(String key, String value) {
        this.env.add(key + "=" + value);

    }
    public void run(boolean displayCmd) throws Exception {
        run(displayCmd, true);
    }
}

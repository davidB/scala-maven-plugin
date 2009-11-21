package org.scala_tools.maven.executions;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

import org.apache.maven.plugin.AbstractMojo;
import org.codehaus.plexus.util.StringUtils;
/**
 * This class will call a java main method via reflection.
 *
 * @author J. Suereth
 *
 * Note: a -classpath argument *must* be passed into the jvmargs.
 *
 */
public class JavaMainCallerInProcess extends JavaMainCallerSupport {

    private ClassLoader _cl = null;

    public JavaMainCallerInProcess(AbstractMojo requester,  String mainClassName, String classpath, String[] jvmArgs, String[] args) throws Exception {
        super(requester, mainClassName, "", jvmArgs, args);

        //Pull out classpath and create class loader
        ArrayList<URL> urls = new ArrayList<URL>();
        for(String path : classpath.split(File.pathSeparator)) {
            try {
                urls.add(new File(path).toURI().toURL());
            } catch (MalformedURLException e) {
                //TODO - Do something usefull here...
                requester.getLog().error(e);
            }
        }
        _cl = new URLClassLoader(urls.toArray(new URL[urls.size()]), (ClassLoader)null);
    }



    public void addJvmArgs(String... args) {
        //TODO - Ignore classpath
        if (args != null) {
            for (String arg : args) {
                requester.getLog().warn("jvmArgs are ignored when run in process :" + arg);
            }
        }
    }

    public boolean run(boolean displayCmd, boolean throwFailure) throws Exception {
        try {
            runInternal(displayCmd);
            return true;
        } catch (Exception e) {
            if(throwFailure) {
                throw e;
            } else {
                return false;
            }
        }
    }

    /** spawns a thread to run the method */
    public void spawn(final boolean displayCmd) throws Exception {
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    runInternal(displayCmd);
                } catch (Exception e) {
                    // Ignore
                }
            }
        };
        t.start();
    }

    /** Runs the main method of a java class */
    private void runInternal(boolean displayCmd) throws Exception {
        String[] argArray = args.toArray(new String[args.size()]);
        if(displayCmd) {
            requester.getLog().info("cmd : " + mainClassName + "(" + StringUtils.join(argArray, ",")+")");
        }
        MainHelper.runMain(mainClassName, args, _cl);
    }


}

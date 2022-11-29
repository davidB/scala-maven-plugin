/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package scala_maven_executions;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;

/**
 * This class will call a java main method via reflection.
 *
 * @author J. Suereth
 *     <p>Note: a -classpath argument *must* be passed into the jvmargs.
 */
public class JavaMainCallerInProcess extends JavaMainCallerSupport {

  private ClassLoader _cl;

  public JavaMainCallerInProcess(
      Log mavenLogger, String mainClassName, String classpath, String[] jvmArgs, String[] args)
      throws Exception {
    super(mavenLogger, mainClassName, "", jvmArgs, args);

    // Pull out classpath and create class loader
    ArrayList<URL> urls = new ArrayList<>();
    for (String path : classpath.split(File.pathSeparator)) {
      try {
        urls.add(new File(path).toURI().toURL());
      } catch (MalformedURLException e) {
        // TODO - Do something usefull here...
        mavenLogger.error(e);
      }
    }
    _cl = new URLClassLoader(urls.toArray(new URL[] {}), null);
  }

  @Override
  public void addJvmArgs(String... args0) {
    // TODO - Ignore classpath
    if (args0 != null) {
      for (String arg : args0) {
        mavenLogger.warn("jvmArgs are ignored when run in process :" + arg);
      }
    }
  }

  @Override
  public boolean run(boolean displayCmd, boolean throwFailure) throws Exception {
    try {
      runInternal(displayCmd);
      return true;
    } catch (Exception e) {
      if (throwFailure) {
        throw e;
      }
      return false;
    }
  }

  /** spawns a thread to run the method */
  @Override
  public SpawnMonitor spawn(final boolean displayCmd) {
    final Thread t =
        new Thread(
            () -> {
              try {
                runInternal(displayCmd);
              } catch (Exception e) {
                // Ignore
              }
            });
    t.start();
    return t::isAlive;
  }

  /** Runs the main method of a java class */
  private void runInternal(boolean displayCmd) throws Exception {
    String[] argArray = args.toArray(new String[] {});
    if (displayCmd) {
      mavenLogger.info("cmd : " + mainClassName + "(" + StringUtils.join(argArray, ",") + ")");
    }
    MainHelper.runMain(mainClassName, args, _cl);
  }

  @Override
  public void redirectToLog() {
    mavenLogger.warn("redirection to log is not supported for 'inProcess' mode");
  }
}

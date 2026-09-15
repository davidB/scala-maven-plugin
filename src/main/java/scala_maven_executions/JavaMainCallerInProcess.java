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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;

/**
 * Runs a Scala tool in the current JVM via reflection, invoking either its {@code main} or its
 * non-exiting {@code process} entry point (see {@link EntryPoint}). {@code PROCESS} is used for the
 * compiler so that an in-process build survives its would-be {@code System.exit}.
 *
 * @author J. Suereth
 *     <p>Note: a -classpath argument *must* be passed into the jvmargs.
 */
public class JavaMainCallerInProcess extends JavaMainCallerSupport {

  // Cache compiler classloaders by classpath so the compiler loads once and stays JIT-warm across
  // modules. Safe to share across concurrent compiles because each uses a fresh driver instance.
  private static final Map<String, ClassLoader> COMPILER_CLASSLOADERS = new ConcurrentHashMap<>();

  private final EntryPoint entryPoint;
  private final String driverClassName;
  private ClassLoader _cl;

  public JavaMainCallerInProcess(
      Log mavenLogger,
      String mainClassName,
      String classpath,
      String[] jvmArgs,
      String[] args,
      EntryPoint entryPoint,
      boolean reuseInProcessCompiler,
      String driverClassName)
      throws Exception {
    super(mavenLogger, mainClassName, "", jvmArgs, args);
    this.entryPoint = entryPoint;
    this.driverClassName = driverClassName;
    // Reuse a shared warm classloader only for the compiler (PROCESS); the MAIN path invokes a
    // singleton main and so gets a fresh classloader.
    boolean reuse = reuseInProcessCompiler && entryPoint == EntryPoint.PROCESS;
    _cl =
        reuse
            ? COMPILER_CLASSLOADERS.computeIfAbsent(
                classpath, cp -> buildClassLoader(cp, mavenLogger))
            : buildClassLoader(classpath, mavenLogger);
  }

  private static ClassLoader buildClassLoader(String classpath, Log mavenLogger) {
    ArrayList<URL> urls = new ArrayList<>();
    for (String path : classpath.split(File.pathSeparator)) {
      try {
        urls.add(new File(path).toURI().toURL());
      } catch (MalformedURLException e) {
        // TODO - Do something usefull here...
        mavenLogger.error(e);
      }
    }
    return new URLClassLoader(urls.toArray(new URL[] {}), null);
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
      return runInternal(displayCmd);
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

  /** Invokes the selected entry point in-process; returns true on success. */
  private boolean runInternal(boolean displayCmd) throws Exception {
    if (displayCmd) {
      String[] argArray = args.toArray(new String[] {});
      mavenLogger.info("cmd : " + mainClassName + "(" + StringUtils.join(argArray, ",") + ")");
    }
    switch (entryPoint) {
      case PROCESS:
        return ProcessHelper.runProcess(driverClassName, args, _cl);
      case MAIN:
      default:
        MainHelper.runMain(mainClassName, args, _cl);
        return true;
    }
  }

  @Override
  public void redirectToLog() {
    // No-op: in-process the compiler runs in the Maven JVM, so its output already reaches the
    // same console/log — there is no separate child-process stream to redirect.
  }

  /** Which entry point {@link JavaMainCallerInProcess} invokes on the target class. */
  public enum EntryPoint {
    /** The standard {@code static void main(String[])}. */
    MAIN,
    /**
     * The compiler's non-exiting {@code process(String[])} — the counterpart of {@code main} that
     * returns instead of calling {@code System.exit}, so an in-process build survives it.
     */
    PROCESS
  }
}

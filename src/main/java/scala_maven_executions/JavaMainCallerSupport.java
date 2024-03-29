/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package scala_maven_executions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;

/**
 * Abstract helper implementation for JavaMainCaller interface.
 *
 * @author josh
 */
public abstract class JavaMainCallerSupport implements JavaMainCaller {

  protected Log mavenLogger;
  protected List<String> env = new ArrayList<>();
  protected String mainClassName;
  protected List<String> jvmArgs = new ArrayList<>();
  protected List<String> args = new ArrayList<>();

  protected JavaMainCallerSupport(
      Log mavenLogger, String mainClassName1, String classpath, String[] jvmArgs1, String[] args1) {
    this.mavenLogger = mavenLogger;
    for (String key : System.getenv().keySet()) {
      env.add(key + "=" + System.getenv(key));
    }
    this.mainClassName = mainClassName1;
    if (StringUtils.isNotEmpty(classpath)) {
      addJvmArgs("-classpath", classpath);
    }
    addJvmArgs(jvmArgs1);
    addArgs(args1);
  }

  @Override
  public void addJvmArgs(String... args0) {
    if (args0 != null) {
      for (String arg : args0) {
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
    boolean found = false;
    boolean isClasspath = false;
    for (int i = 0; i < jvmArgs.size(); i++) {
      String item = jvmArgs.get(i);
      if (isClasspath) {
        item = item + File.pathSeparator + entry.getCanonicalPath();
        jvmArgs.set(i, item);
        found = true;
        break;
      }
      isClasspath = "-classpath".equals(item);
    }
    if (!found) {
      addJvmArgs("-classpath", entry.getCanonicalPath());
    }
  }

  @Override
  public void addOption(String key, String value) {
    if (StringUtils.isEmpty(value) || StringUtils.isEmpty(key)) {
      return;
    }
    addArgs(key, value);
  }

  @Override
  public void addOption(String key, File value) {
    if ((value == null) || StringUtils.isEmpty(key)) {
      return;
    }
    addArgs(key, value.getAbsolutePath());
  }

  @Override
  public void addOption(String key, boolean value) {
    if ((!value) || StringUtils.isEmpty(key)) {
      return;
    }
    addArgs(key);
  }

  @Override
  public void addArgs(String... args1) {
    if (args1 != null) {
      for (String arg : args1) {
        if (StringUtils.isNotEmpty(arg)) {
          this.args.add(arg);
        }
      }
    }
  }

  @Override
  public void run(boolean displayCmd) throws Exception {
    run(displayCmd, true);
  }
}

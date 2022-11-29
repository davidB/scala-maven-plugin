/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package scala_maven;

import java.io.File;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.util.StringUtils;
import scala_maven_executions.JavaMainCaller;
import scala_maven_executions.JavaMainCallerByFork;
import util.FileUtils;
import util.JavaLocator;

/** Run a Scala class using the Scala runtime */
@Mojo(name = "run", requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true)
@Execute(phase = LifecyclePhase.TEST_COMPILE)
public class ScalaRunMojo extends ScalaMojoSupport {

  /** The class to use when launching a scala program */
  @Parameter(property = "launcher")
  private String launcher;

  /**
   * Additional parameter to use to call the main class Using this parameter only from command line
   * ("-DaddArgs=arg1|arg2|arg3|..."), not from pom.xml.
   */
  @Parameter(property = "addArgs")
  private String addArgs;

  /**
   * A list of launcher definition (to avoid rewriting long command line or share way to call an
   * application) launchers could be define by :
   *
   * <pre>
   *   &lt;launchers&gt;
   *     &lt;launcher&gt;
   *       &lt;id&gt;myLauncher&lt;/id&gt;
   *       &lt;mainClass&gt;my.project.Main&lt;/mainClass&gt;
   *       &lt;args&gt;
   *         &lt;arg&gt;arg1&lt;/arg&gt;
   *       &lt;/args&gt;
   *       &lt;jvmArgs&gt;
   *         &lt;jvmArg&gt;-Xmx64m&lt;/jvmArg&gt;
   *       &lt;/jvmArgs&gt;
   *     &lt;/launcher&gt;
   *     &lt;launcher&gt;
   *       &lt;id&gt;myLauncher2&lt;/id&gt;
   *       ...
   *       &lt;&gt;&lt;&gt;
   *     &lt;/launcher&gt;
   *   &lt;/launchers&gt;
   * </pre>
   */
  @Parameter private Launcher[] launchers;

  /**
   * Main class to call, the call use the jvmArgs and args define in the pom.xml, and the addArgs
   * define in the command line if define.
   *
   * <p>Higher priority to launcher parameter) Using this parameter only from command line
   * (-DmainClass=...), not from pom.xml.
   */
  @Parameter(property = "mainClass")
  private String mainClass;

  @Override
  protected void doExecute() throws Exception {
    JavaMainCaller jcmd = null;
    File javaExec = JavaLocator.findExecutableFromToolchain(getToolchain());
    if (StringUtils.isNotEmpty(mainClass)) {
      jcmd =
          new JavaMainCallerByFork(
              getLog(),
              mainClass,
              FileUtils.toMultiPath(FileUtils.fromStrings(project.getTestClasspathElements())),
              jvmArgs,
              args,
              forceUseArgFile,
              javaExec);
    } else if ((launchers != null) && (launchers.length > 0)) {
      if (StringUtils.isNotEmpty(launcher)) {
        for (int i = 0; (i < launchers.length) && (jcmd == null); i++) {
          if (launcher.equals(launchers[i].id)) {
            getLog()
                .info("launcher '" + launchers[i].id + "' selected => " + launchers[i].mainClass);
            jcmd =
                new JavaMainCallerByFork(
                    getLog(),
                    launchers[i].mainClass,
                    FileUtils.toMultiPath(
                        FileUtils.fromStrings(project.getTestClasspathElements())),
                    launchers[i].jvmArgs,
                    launchers[i].args,
                    forceUseArgFile,
                    javaExec);
          }
        }
      } else {
        getLog().info("launcher '" + launchers[0].id + "' selected => " + launchers[0].mainClass);
        jcmd =
            new JavaMainCallerByFork(
                getLog(),
                launchers[0].mainClass,
                FileUtils.toMultiPath(FileUtils.fromStrings(project.getTestClasspathElements())),
                launchers[0].jvmArgs,
                launchers[0].args,
                forceUseArgFile,
                javaExec);
      }
    }
    if (jcmd != null) {
      if (StringUtils.isNotEmpty(addArgs)) {
        jcmd.addArgs(StringUtils.split(addArgs, "|"));
      }
      jcmd.run(displayCmd);
    } else {
      getLog().warn("Not mainClass or valid launcher found/define");
    }
  }
}

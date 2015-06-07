package scala_maven;

import scala_maven_executions.JavaMainCaller;


/**
 * Display the Scala Compiler help
 *
 * @goal help
 */
public class ScalaHelpMojo extends ScalaMojoSupport {
    /**
     * Determines if help will only display a version
     * @parameter property="maven.scala.help.versionOnly" default-value="false"
     */
    private boolean versionOnly;

    @Override
    public void doExecute() throws Exception {
        JavaMainCaller jcmd = null;
        if (!versionOnly) {
            jcmd = getScalaCommand();
            jcmd.addArgs("-help");
            jcmd.addArgs("-X");
            jcmd.addArgs("-Y");
            jcmd.run(displayCmd);
        }
        jcmd = getScalaCommand();
        jcmd.addArgs("-version");
        jcmd.run(displayCmd);
    }
}

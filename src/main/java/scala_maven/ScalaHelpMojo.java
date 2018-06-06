package scala_maven;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import scala_maven_executions.JavaMainCaller;


/**
 * Display the Scala Compiler help
 *
 */
@Mojo(name = "help")
public class ScalaHelpMojo extends ScalaMojoSupport {
    /**
     * Determines if help will only display a version
     *
     */
    @Parameter(property="maven.scala.help.versionOnly", defaultValue="false")
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

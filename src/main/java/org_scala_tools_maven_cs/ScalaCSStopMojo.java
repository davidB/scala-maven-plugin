package org_scala_tools_maven_cs;


/**
 * Request compile to ScalaCS (no more, doesn't create project, check if exist,...)
 * @goal cs-stop
 */
public class ScalaCSStopMojo extends ScalaCSMojoSupport {

    @Override
    protected void doExecute() throws Exception {
        super.doExecute();
        System.out.println(scs.sendRequestStop());
    }
}

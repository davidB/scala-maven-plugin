package org_scala_tools_maven_cs;


/**
 * Request compile to ScalaCS (no more, doesn't create project, check if exist,...)
 * @goal cs-stop
 */
public class ScalaCSStopMojo extends ScalaCSMojoSupport {

    @Override
    protected CharSequence doRequest() throws Exception {
        return scs.sendRequestStop();
    }
}

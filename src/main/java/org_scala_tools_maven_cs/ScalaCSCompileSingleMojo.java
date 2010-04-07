package org_scala_tools_maven_cs;


/**
 * Request compile to ScalaCS (no more, doesn't create project, check if exist,...), but only the current project
 * @goal cs-compile-single
 */
public class ScalaCSCompileSingleMojo extends ScalaCSMojoSupport {

    @Override
    protected CharSequence doRequest() throws Exception {
        return scs.sendRequestCompile(projectNamePattern(), false, false);
    }
}

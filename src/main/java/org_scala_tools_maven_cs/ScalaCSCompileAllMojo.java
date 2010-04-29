package org_scala_tools_maven_cs;


/**
 * Request compile to ScalaCS (no more, doesn't create project, check if exist, request compile of dependencies,...)
 * @goal cs-compile-all
 */
public class ScalaCSCompileAllMojo extends ScalaCSMojoSupport {

    @Override
    protected CharSequence doRequest() throws Exception {
        return scs.sendRequestCompile(null, true, true);
    }
}

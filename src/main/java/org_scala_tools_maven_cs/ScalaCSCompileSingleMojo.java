package org_scala_tools_maven_cs;


/**
 * Request compile to ScalaCS (no more, doesn't create project, check if exist,...), but only the current project
 * @goal cs-compile-single
 */
public class ScalaCSCompileSingleMojo extends ScalaCSMojoSupport {

    @Override
    protected void doExecute() throws Exception {
        super.doExecute();
        System.out.println(scs.sendRequestCompile(project.getArtifactId()+"-"+project.getVersion(), false, false));
    }
}

package org_scala_tools_maven_cs;


/**
 * Request compile to ScalaCS (no more, doesn't create project, check if exist, request compile of dependencies,...)
 * @goal cs-compile
 */
public class ScalaCSCompileMojo extends ScalaCSMojoSupport {

    @Override
    protected void doExecute() throws Exception {
        super.doExecute();
        System.out.println(scs.sendRequestCompile(project.getArtifactId()+"-"+project.getVersion(), true, true));
    }
}

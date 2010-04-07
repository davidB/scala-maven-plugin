package org_scala_tools_maven_cs;

import org.apache.maven.plugin.MojoFailureException;

import org_scala_tools_maven.ScalaMojoSupport;

abstract public class ScalaCSMojoSupport extends ScalaMojoSupport {

    /**
     * If you want to use an other version of scalacs than the default one.
     *
     * @parameter expression="${maven.scalacs.version}"
     */
    protected String csVersion = "0.2-SNAPSHOT";

    protected ScalacsClient scs;

    @Override
    final protected void doExecute() throws Exception {
        scs = new ScalacsClient(this, csVersion, jvmArgs);
        String output = doRequest().toString();
        //TODO use parser and maven logger to print (and find warning, error,...)
        //TODO use Stream instead of String to allow progressive display (when scalacs will support it)
        System.out.println(output);
        if (output.contains("-ERROR")) {
            throw new MojoFailureException("ScalaCS reply with ERRORs");
        }
    }

    protected abstract CharSequence doRequest() throws Exception;

    protected String projectNamePattern() throws Exception {
        return project.getArtifactId() + "-" + project.getVersion() + "/.*";
    }
}

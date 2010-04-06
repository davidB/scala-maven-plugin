package org_scala_tools_maven_cs;

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
    protected void doExecute() throws Exception {
        scs = new ScalacsClient(this, csVersion, jvmArgs);
    }
}

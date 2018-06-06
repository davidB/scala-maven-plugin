package scala_maven;

import org.apache.maven.plugins.annotations.Parameter;

public class Launcher {
    protected String id;

    protected String mainClass;

    /**
     * Jvm Arguments
     */
    @Parameter
    protected String[] jvmArgs;

    /**
     * compiler additionnals arguments
     */
    @Parameter
    protected String[] args;
}

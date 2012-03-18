package scala_maven;

public class Launcher {
    protected String id;

    protected String mainClass;

    /**
     * Jvm Arguments
     *
     * @parameter
     */
    protected String[] jvmArgs;

    /**
     * compiler additionnals arguments
     *
     * @parameter
     */
    protected String[] args;
}

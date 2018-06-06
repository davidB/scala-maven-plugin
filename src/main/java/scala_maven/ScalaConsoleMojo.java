package scala_maven;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import scala_maven_executions.JavaMainCaller;
import scala_maven_executions.MainHelper;

/**
 * Run the Scala console with all the classes of the projects (dependencies and
 * builded)
 *
 */
@Mojo(name = "console", requiresDependencyResolution = ResolutionScope.TEST, inheritByDefault = false, requiresDirectInvocation = true, executionStrategy = "once-per-session")
public class ScalaConsoleMojo extends ScalaMojoSupport {

    // Private Static Values //

    /**
     * Constant {@link String} for "jline". Used for the artifact id, and
     * usually the group id, for the JLine library needed by the Scala Console.
     */
    private static final String JLINE = "jline";

    /**
     * Constant {@link String} for "org.scala-lang". In this class it is used
     * for the forked JLine group id.
     */
    private static final String SCALA_ORG_GROUP = "org.scala-lang";

    // Instance Members //

    /**
     * The console to run.
     *
     */
    @Parameter(property = "mainConsole", defaultValue = "scala.tools.nsc.MainGenericRunner", required = true)
    protected String mainConsole;

    /**
     * Add the test classpath (include classes from test directory), to the
     * console's classpath ?
     *
     */
    @Parameter(property = "maven.scala.console.useTestClasspath", defaultValue = "true", required = true)
    protected boolean useTestClasspath;

    /**
     * Add the runtime classpath, to the console's classpath ?
     *
     */
    @Parameter(property = "maven.scala.console.useRuntimeClasspath", defaultValue = "true", required = true)
    protected boolean useRuntimeClasspath;

    /**
     * Path of the javaRebel jar. If this option is set then the console run
     * with <a href="http://www.zeroturnaround.com/javarebel/">javarebel</a>
     * enabled.
     *
     */
    @Parameter(property = "javarebel.jar.path")
    protected File javaRebelPath;

    @Override
    protected void doExecute() throws Exception {
        // Force no forking
        final JavaMainCaller jcmd = super.getScalaCommand(false, this.mainConsole);
        // Determine Scala Version
        final VersionNumber scalaVersion = super.findScalaVersion();
        final Set<String> classpath = this.setupClassPathForConsole(scalaVersion);

        // Log if we are violating the user settings.
        if (super.fork) {
            super.getLog().info("Ignoring fork for console execution.");
        }

        // Setup the classpath

        // Build the classpath string.
        final String classpathStr = MainHelper.toMultiPath(classpath.toArray(new String[classpath.size()]));

        // Setup the JavaMainCaller
        jcmd.addArgs(super.args);
        jcmd.addOption("-cp", classpathStr);
        super.addCompilerPluginOptions(jcmd);

        // Check for Java Rebel
        this.handleJavaRebel(jcmd);

        // Run
        jcmd.run(super.displayCmd);
    }

    // Private Methods //

    /**
     * If {@link #javaRebelPath} is defined, then attempt to resolve it on the
     * filesystem and setup the Scala console to use it.
     * <p>
     * If we are unable to find it or {@link #javaRebelPath} is {@code null},
     * then nothing is done to the given {@link JavaMainCaller}.
     *
     * @param jcmd the {@link JavaMainCaller} to which to add the JRebel settings.
     */
    private void handleJavaRebel(final JavaMainCaller jcmd) throws IOException {
        if (this.javaRebelPath != null) {
            final String canonicalJavaRebelPath = this.javaRebelPath.getCanonicalPath();
            if (this.javaRebelPath.exists()) {
                jcmd.addJvmArgs("-noverify", String.format("-javaagent:%s", canonicalJavaRebelPath));
            } else {
                super.getLog().warn(String.format("javaRevelPath '%s' not found", canonicalJavaRebelPath));
            }
        }
    }

    /**
     * Construct the appropriate Classpath for the Scala console.
     *
     * @param scalaVersion the {@link VersionNumber} for the Scala
     *                     Compiler/Library we are using.
     *
     * @return A {@link Set} of {@link String} defining classpath values to
     *         provide to the Scala console.
     *
     * @throws {@link Exception} for many reasons, mostly relating to ad-hoc
     *         dependency resolution.
     */
    private Set<String> setupClassPathForConsole(final VersionNumber scalaVersion) throws Exception {
        final Set<String> classpath = new HashSet<String>();

        classpath.addAll(this.setupProjectClasspaths());
        classpath.addAll(this.setupConsoleClasspaths(scalaVersion));

        return classpath;
    }

    /**
     * Construct the Classpath defined by the project and plugin settings.
     * <p>
     * This should include the following entities.
     * <ul>
     *   <li> Scala Compiler, as defined by {@link ScalaMojoSupport#addCompilerToClasspath}.
     *   <li> Library Classpath, as defined by {@link ScalaMojoSupport#addLibraryToClasspath}.
     *   <li> Test Classpath, if {@link #useTestClasspath} is {@code true}.
     *   <li> Runtime Classpath, if {@link #useRuntimeClasspath} is {@code true}.
     * </ul>
     *
     * @return A {@link Set} of {@link String} defining the classpath for the
     *         project and plugin settings.
     *
     * @throws {@link Exception} for many reasons, mostly relating to ad-hoc
     *         dependency resolution.
     */
    private Set<String> setupProjectClasspaths() throws Exception {
        final Set<String> classpath = new HashSet<String>();

        super.addCompilerToClasspath(classpath);
        super.addLibraryToClasspath(classpath);

        if (this.useTestClasspath) {
            classpath.addAll(super.project.getTestClasspathElements());
        }

        if (this.useRuntimeClasspath) {
            classpath.addAll(super.project.getRuntimeClasspathElements());
        }

        return classpath;
    }

    /**
     * Construct the Classpath for any additional dependencies that are needed
     * to run the Scala console.
     * <p>
     * This should include the following entities.
     * <ul>
     *   <li> Jline, used for readline like features in the REPL.
     * </ul>
     *
     * @param scalaVersion the version of the Scala Compiler/Library we are
     *        using for this execution.
     *
     * @return A {@link Set} of {@link String} of the classpath as defined by
     *
     * @throws {@link Exception} for many reasons, mostly relating to ad-hoc
     *         dependency resolution.
     */
    private Set<String> setupConsoleClasspaths(final VersionNumber scalaVersion) throws Exception {
        final Set<String> classpath = new HashSet<String>();

        super.addToClasspath(this.resolveJLine(scalaVersion, this.fallbackJLine(scalaVersion)), classpath, true);

        return classpath;
    }

    /**
     * Attempt to resolve JLine against the Scala Compiler's dependency tree.
     * <p>
     * This allows us to not have to worry about manually keeping the JLine
     * dependency synchronized with the Scala REPL, which is likely to cause
     * binary compatibility errors. If, for some reason, we are unable to
     * resolve this dependency using the Compiler's dependency tree, we fallback
     * to a set of hard-coded defaults that will <em>usually</em> work, but will
     * not always work.
     * <p>
     * If the dynamic approach to finding the JLine dependency proves stable, we
     * may drop the fallback in the future.
     *
     * @param scalaVersion the version of the Scala Compiler/Library we are
     *        using for this execution.
     * @param defaultFallback returned if we are unable to resolve JLine against
     *        the Scala Compiler's dependency tree.
     *
     * @return an {@link Artifact} to provide to the runtime of the Scala
     *         console conforming the JLine.
     *
     * @throws {@link Exception} for many reasons, mostly relating to ad-hoc
     *         dependency resolution.
     */
    private Artifact resolveJLine(final VersionNumber scalaVersion,
                                  final Artifact defaultFallback) throws Exception {
        final Artifact compilerArtifact =
            super.scalaCompilerArtifact(scalaVersion.toString());
        final Set<Artifact> compilerDeps =
            super.resolveArtifactDependencies(compilerArtifact);
        for (final Artifact a : compilerDeps) {
            if (this.filterForJline(a)) {
                return a;
            }
        }

        super.getLog().warn("Unable to determine the required Jline dependency from the POM. Falling back to hard-coded defaults.");
        super.getLog().warn("If you get an InvocationTargetException, then this probably means we guessed the wrong version for JLine");
        super.getLog().warn(String.format("Guessed JLine: %s", defaultFallback.toString()));

        return defaultFallback;
    }

    /**
     * Helper function to filter a collection of {@link Artifact} for JLine.
     * <p>
     * Since different versions of the Scala Compiler were using different
     * artifacts for JLine, things are a bit tricky here. Any {@link Artifact}
     * to have an artifact id equal to {@code "jline"} and a group id equal to
     * <em>either</em> {@code "jline"} or {@code "org.scala-lang"} will yield
     * {@code true}. The latter group id corresponds to a fork of JLine that is
     * not being used anymore.
     *
     * @param artifact the {@link Artifact} to check to see if it is viable
     *        JLine candidate.
     */
    private boolean filterForJline(final Artifact artifact) {
        final String artifactId = artifact.getArtifactId();
        final String groupId = artifact.getGroupId();

        if (artifactId.equals(ScalaConsoleMojo.JLINE) &&
            (groupId.equals(ScalaConsoleMojo.JLINE) ||
             groupId.equals(ScalaConsoleMojo.JLINE))) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Hard coded fallback values for JLine. This used to be the only way we
     * resolve JLine, but required manually upkeep to avoid binary
     * comparability errors.
     * <p>
     * You should favor {@link #resolveJLine} only using this for a fallback
     * default.
     * <p>
     * Current mapping is as follows.
     * <ul>
     *   <li>Scala 2.12.0-M4 and after, jline:jline:2.14.1:jar</li>
     *   <li>After Scala 2.11.0 through Scala 2.12.0-M3, jline:jline:2.12:jar</li>
     *   <li>After Scala 2.9.0 before Scala 2.11.0, org.scala-lang:jline:SCALA-VERSION:jar</li>
     *   <li>Before Scala 2.9.0, jline:jline:0.9.94:jar</li>
     * </ul>
     *
     * @param scalaVersion the version of the Scala Compiler/Library we are
     *        using for this execution.
     *
     * @return a fallback {@link Artifact} which we <em>hope</em> will work with
     *         the Scala REPL.
     */
    private Artifact fallbackJLine(final VersionNumber scalaVersion) {
        // https://github.com/scala/scala/blob/365ac035a863a666f86151371db77c6d401e88a2/versions.properties#L29
        final VersionNumber scala2_12_0M4 = new VersionNumber("2.12.0-M4");
        final VersionNumber scala2_11_0 = new VersionNumber("2.11.0");
        final VersionNumber scala2_9_0 = new VersionNumber("2.9.0");

        if (scala2_12_0M4.compareTo(scalaVersion) <= 0) {
            return super.factory.createArtifact(ScalaConsoleMojo.JLINE, ScalaConsoleMojo.JLINE, "2.14.1", "", ScalaMojoSupport.JAR);
        } else if (scala2_11_0.compareTo(scalaVersion) <= 0) {
            return super.factory.createArtifact(ScalaConsoleMojo.JLINE, ScalaConsoleMojo.JLINE, "2.12", "", ScalaMojoSupport.JAR);
        } else if (scala2_9_0.compareTo(scalaVersion) <= 0) {
            return super.factory.createArtifact(ScalaConsoleMojo.SCALA_ORG_GROUP, ScalaConsoleMojo.JLINE, scalaVersion.toString(), "", ScalaMojoSupport.JAR);
        } else {
            return super.factory.createArtifact(ScalaConsoleMojo.JLINE, ScalaConsoleMojo.JLINE, "0.9.94", "", ScalaMojoSupport.JAR);
        }
    }
}

package scala_maven;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.maven.BuildFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.invoker.CommandLineConfigurationException;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenCommandLineBuilder;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.apache.maven.shared.invoker.SystemOutHandler;
import org.codehaus.plexus.util.StringUtils;

/**
 * Compile the main and test scala source directory then run unit test cases in
 * continuous (infinite loop).
 * This is an util goal for commandline usage only (Do not use or call it in a
 * pom) !!!
 *
 */
@Mojo(name = "cctest", requiresDependencyResolution = ResolutionScope.TEST)
public class ScalaContinuousTestMojo extends ScalaContinuousCompileMojo {

    @Component
    protected Invoker invoker;

    /**
     * The local repository for caching artifacts. It is strongly recommended to
     * specify a path to an isolated
     * repository like <code>${project.build.directory}/it-repo</code>. Otherwise,
     * your ordinary local repository will
     * be used, potentially soiling it with broken artifacts.
     *
     */
    @Parameter(property = "invoker.localRepositoryPath", defaultValue = "${settings.localRepository}")
    protected File localRepositoryPath;

    /**
     * Specify this parameter to run individual tests by file name, overriding the
     * <code>includes/excludes</code>
     * parameters. Each pattern you specify here will be used to create an
     * include pattern formatted like <code>**&#47;${test}.java</code>, so you can
     * just type "-Dtest=MyTest"
     * to run a single test called "foo/MyTest.java". This parameter will override
     * the TestNG suiteXmlFiles
     * parameter.
     *
     */
    @Parameter(property = "test")
    protected String test;

    /**
     * A space-separated list of the goals to execute as part of running the tests.
     * You can use this
     * setting to run different testing tools other than just JUnit. For example, to
     * run the
     * ScalaTest (with the maven-scalatest-plugin):
     *
     * <pre>
     *   mvn -Dcctest.goals=scalatest:test scala:cctest
     * </pre>
     *
     * To run both ScalaTest and JUnit tests:
     *
     * <pre>
     *   mvn -Dcctest.goals="surefire:test scalatest:test" scala:cctest
     * </pre>
     *
     * If you need to specify the goal every time you run <code>scala:cctest</code>,
     * you can
     * configure the setting in the pom.xml:
     *
     * <pre>
     *    &lt;plugin&gt;
     *       &lt;groupId&gt;net.alchim31.maven&lt;/groupId&gt;
     *       &lt;artifactId&gt;scala-maven-plugin&lt;/artifactId&gt;
     *       &lt;version&gt;2.16.0&lt;/version&gt;
     *       &lt;configuration&gt;
     *          &lt;ccTestGoals&gt;scalatest:test&lt;/ccTestGoals&gt;
     *       &lt;/configuration&gt;
     *       &lt;!-- normal executions here --&gt;
     *    &lt;/plugin&gt;
     * </pre>
     *
     */
    @Parameter(property = "cctest.goals", defaultValue = "surefire:test")
    protected String ccTestGoals;

    @Override
    protected void postCompileActions() throws Exception {
        if (test == null) {
            getLog().info("Now running all the unit tests. Use -Dtest=FooTest to run a single test by name");
        }
        else {
            getLog().info("Now running tests matching: " + test);
        }

        final InvocationRequest request = new DefaultInvocationRequest();
        request.setLocalRepositoryDirectory(localRepositoryPath);
        request.setBatchMode(true);
        request.setErrorHandler(new SystemOutHandler(true));
        request.setOutputHandler(new SystemOutHandler(true));
        request.setBaseDirectory(project.getBasedir());
        request.setPomFile(new File(project.getBasedir(), "pom.xml"));

        request.setGoals(getMavenGoals());
        request.setOffline(false);

        if (test != null) {
            Properties properties = new Properties();
            properties.put("test", test);
            request.setProperties(properties);
        }


        if (getLog().isDebugEnabled()) {
            try {
                getLog().debug("Executing: " + new MavenCommandLineBuilder().build(request));
            }
            catch (CommandLineConfigurationException e) {
                getLog().debug("Failed to display command line: " + e.getMessage());
            }
        }

        try {
            invoker.execute(request);
        }
        catch (final MavenInvocationException e) {
            getLog().debug("Error invoking Maven: " + e.getMessage(), e);
            throw new BuildFailureException("Maven invocation failed. " + e.getMessage(), e);
        }
    }

    protected List<String> getMavenGoals() {
        getLog().debug("Running tests with goal(s): " + ccTestGoals);
        return Arrays.asList(StringUtils.split(ccTestGoals, " "));
    }
}

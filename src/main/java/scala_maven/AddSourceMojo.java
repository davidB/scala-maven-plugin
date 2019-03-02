package scala_maven;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Add more source directories to the POM.
 */
@Mojo(name = "add-source", executionStrategy = "always", defaultPhase = LifecyclePhase.INITIALIZE, threadSafe = true)
public class AddSourceMojo extends AbstractMojo {

    /**
     * The maven project
     */
    @Parameter(property = "project", required = true, readonly = true)
    private MavenProject project;

    /**
     * The directory in which scala source is found
     */
    @Parameter(defaultValue = "${project.build.sourceDirectory}/../scala")
    private File sourceDir;

    /**
     * The directory in which testing scala source is found
     */
    @Parameter(defaultValue = "${project.build.testSourceDirectory}/../scala")
    private File testSourceDir;

    /**
     * Should use CanonicalPath to normalize path (true =&gt; getCanonicalPath,
     * false =&gt; getAbsolutePath)
     *
     * @see <a href="https://github.com/davidB/maven-scala-plugin/issues/50">#50</a>
     */
    @Parameter(property = "maven.scala.useCanonicalPath", defaultValue = "true")
    private boolean useCanonicalPath;

    @Override
    public void execute() {
        try {
            if (sourceDir != null) {
                final String path = FileUtils.pathOf(sourceDir, useCanonicalPath);
                if (!project.getCompileSourceRoots().contains(path)) {
                    getLog().info("Add Source directory: " + path);
                    project.addCompileSourceRoot(path);
                }
            }
            if (testSourceDir != null) {
                final String path = FileUtils.pathOf(testSourceDir, useCanonicalPath);
                if (!project.getTestCompileSourceRoots().contains(path)) {
                    getLog().info("Add Test Source directory: " + path);
                    project.addTestCompileSourceRoot(path);
                }
            }
        } catch (final Exception exc) {
            getLog().warn(exc);
        }
    }
}

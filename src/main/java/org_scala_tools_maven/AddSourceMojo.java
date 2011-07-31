package org_scala_tools_maven;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * Add more source directories to the POM.
 *
 * @executionStrategy always
 * @goal add-source
 * @phase initialize
 * @requiresDirectInvocation false
 */
public class AddSourceMojo extends AbstractMojo {

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The directory in which scala source is found
     *
     * @parameter expression="${project.build.sourceDirectory}/../scala"
     */
    protected File sourceDir;

    /**
     * The directory in which testing scala source is found
     *
     * @parameter expression="${project.build.testSourceDirectory}/../scala"
     */
    protected File testSourceDir;

    public void execute() throws MojoExecutionException {
        try {
            if (sourceDir != null) {
                String path = sourceDir.getAbsolutePath();
                if (!project.getCompileSourceRoots().contains(path)) {
                    getLog().info("Add Source directory: " + path);
                    project.addCompileSourceRoot(path);
                }
            }
            if (testSourceDir != null) {
                String path = testSourceDir.getAbsolutePath();
                if (!project.getTestCompileSourceRoots().contains(path)) {
                    getLog().info("Add Test Source directory: " + path);
                    project.addTestCompileSourceRoot(path);
                }
            }
        } catch(Exception exc) {
            getLog().warn(exc);
        }
    }
}

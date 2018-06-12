package scala_maven;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.ManifestException;

/**
 * Creates a jar of the non-aggregated scaladoc and attaches it
 * to the project for distribution.
 *
 */
@Mojo(name = "doc-jar", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE)
public class ScalaDocJarMojo extends ScalaDocMojo {

    private static final String[] DEFAULT_INCLUDES = new String[] { "**/**" };
    private static final String[] DEFAULT_EXCLUDES = new String[] {};
    /**
     * The Jar archiver.
     *
     */
    @Component(role = Archiver.class, hint = "jar")
    private JarArchiver jarArchiver;

    /**
     * Used for attaching the artifact in the project.
     *
     */
    @Component
    private MavenProjectHelper projectHelper;

    /**
     * Specifies the filename that will be used for the generated jar file. Please
     * note that <code>-javadoc</code>
     * or <code>-test-javadoc</code> will be appended to the file name.
     *
     */
    @Parameter(property = "project.build.finalName")
    private String finalName;

    /**
     * Specifies whether to attach the generated artifact to the project helper.
     * <br/>
     *
     */
    @Parameter(property = "attach", defaultValue = "true")
    private boolean attach;

    /**
     * Specifies the classifier of the generated artifact.
     *
     */
    @Parameter(property = "classifier", defaultValue = "javadoc")
    private String classifier;

    /**
     * Specifies whether to skip generating scaladoc.
     *
     */
    @Parameter(property = "skip", defaultValue = "false")
    private boolean skip;

    /**
     * Specifies the directory where the generated jar file will be put.
     *
     */
    @Parameter(property = "project.build.directory")
    private String jarOutputDirectory;

    /**
     * The archive configuration to use.
     * See <a href="http://maven.apache.org/shared/maven-archiver/index.html">Maven
     * Archiver Reference</a>.
     *
     */
    @Parameter
    private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * Path to the default MANIFEST file to use. It will be used if
     * <code>useDefaultManifestFile</code> is set to <code>true</code>.
     *
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}/META-INF/MANIFEST.MF", required = true, readonly = true)
    private File defaultManifestFile;

    /**
     * Set this to <code>true</code> to enable the use of the
     * <code>defaultManifestFile</code>.
     * <br/>
     *
     */
    @Parameter(defaultValue = "false")
    private boolean useDefaultManifestFile;

    /**
     * Specifies if the build will fail if there are errors during javadoc execution
     * or not.
     *
     * @since 2.5
     */
    @Parameter(property="maven.javadoc.failOnError", defaultValue="true")
    protected boolean failOnError;

    @Override
    public void doExecute() throws Exception {
        if (skip) {
            getLog().info("Skipping javadoc generation");
            return;
        }
        try {
            generate(null, Locale.getDefault());
            if (reportOutputDirectory.exists()) {
                final File outputFile = generateArchive(reportOutputDirectory, finalName + "-" + getClassifier() + ".jar");
                if (!attach) {
                    getLog().info("NOT adding javadoc to attached artifacts list.");
                } else {
                    // TODO: these introduced dependencies on the project are going to become
                    // problematic - can we export it
                    // through metadata instead?
                    projectHelper.attachArtifact(project, "javadoc", getClassifier(), outputFile);
                }
            }
        } catch (final ArchiverException e) {
            failOnError("ArchiverException: Error while creating archive", e);
        } catch (final IOException e) {
            failOnError("IOException: Error while creating archive", e);
        } catch (final MavenReportException e) {
            failOnError("MavenReportException: Error while creating archive", e);
        } catch (final RuntimeException e) {
            failOnError("RuntimeException: Error while creating archive", e);
        }
    }

    /**
     * Method that creates the jar file
     *
     * @param javadocFiles
     *            the directory where the generated jar file will be put
     * @param jarFileName
     *            the filename of the generated jar file
     * @return a File object that contains the generated jar file
     * @throws ArchiverException
     * @throws IOException
     */
    private File generateArchive(File javadocFiles, String jarFileName) throws ArchiverException, IOException {
        final File javadocJar = new File(jarOutputDirectory, jarFileName);
        if (javadocJar.exists()) {
            javadocJar.delete();
        }
        final MavenArchiver archiver = new MavenArchiver();
        archiver.setArchiver(jarArchiver);
        archiver.setOutputFile(javadocJar);
        final File contentDirectory = javadocFiles;
        if (!contentDirectory.exists()) {
            getLog().warn("JAR will be empty - no content was marked for inclusion!");
        } else {
            archiver.getArchiver().addDirectory(contentDirectory, DEFAULT_INCLUDES, DEFAULT_EXCLUDES);
        }
        final List<Resource> resources = project.getBuild().getResources();
        for (final Resource r : resources) {
            if (r.getDirectory().endsWith("maven-shared-archive-resources")) {
                archiver.getArchiver().addDirectory(new File(r.getDirectory()));
            }
        }
        if (useDefaultManifestFile && defaultManifestFile.exists() && archive.getManifestFile() == null) {
            getLog().info("Adding existing MANIFEST to archive. Found under: " + defaultManifestFile.getPath());
            archive.setManifestFile(defaultManifestFile);
        }
        try {
            // we don't want Maven stuff
            archive.setAddMavenDescriptor(false);
            archiver.createArchive(session, project, archive);
        } catch (final ManifestException e) {
            throw new ArchiverException("ManifestException: " + e.getMessage(), e);
        } catch (final DependencyResolutionRequiredException e) {
            throw new ArchiverException("DependencyResolutionRequiredException: " + e.getMessage(), e);
        }
        return javadocJar;
    }

    protected String getClassifier() {
        return classifier;
    }

    protected void failOnError(String prefix, Exception e)
            throws MojoExecutionException {
        if (failOnError) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new MojoExecutionException(prefix + ": " + e.getMessage(), e);
        }
        getLog().error(prefix + ": " + e.getMessage(), e);
    }
}

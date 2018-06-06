package scala_maven;

import java.io.File;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Compile Scala test source into test-classes. Corresponds roughly to
 * testCompile
 * in maven-compiler-plugin
 *
 */
@Mojo(name = "testCompile", defaultPhase = LifecyclePhase.TEST_COMPILE, requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true)
public class ScalaTestCompileMojo extends ScalaCompilerSupport {

    /**
     * Set this to 'true' to bypass unit tests entirely.
     * Its use is NOT RECOMMENDED, but quite convenient on occasion.
     *
     */
    @Parameter(property = "maven.test.skip")
    protected boolean skip;

    /**
     * The directory in which to place test compilation output
     *
     */
    @Parameter(defaultValue="${project.build.testOutputDirectory}")
    protected File testOutputDir;

    /**
     * The directory in which to find test scala source code
     *
     */
    @Parameter(defaultValue="${project.build.testSourceDirectory}/../scala")
    protected File testSourceDir;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            return;
        }
        super.execute();
    }

    /**
     * Analysis cache file for incremental recompilation.
     *
     */
    @Parameter(property="testAnalysisCacheFile", defaultValue="${project.build.directory}/analysis/test-compile")
    protected File testAnalysisCacheFile;

    @Override
    protected List<String> getClasspathElements() throws Exception {
      List<String> back = project.getTestClasspathElements();
      back.remove(project.getBuild().getTestOutputDirectory());
      //back.add(getOutputDir().getAbsolutePath());
      return back;
    }

    @Override
    protected List<Dependency> getDependencies() {
        return project.getTestDependencies();
    }

    @Override
    protected File getOutputDir() throws Exception {
        return testOutputDir.getAbsoluteFile();
    }

    @Override
    protected List<File> getSourceDirectories() throws Exception {
        List<String> sources = project.getTestCompileSourceRoots();
        String scalaSourceDir = testSourceDir.getAbsolutePath();
        if(!sources.contains(scalaSourceDir)) {
            sources.add(scalaSourceDir);
        }
        return normalize(sources);
    }

    @Override
    protected File getAnalysisCacheFile() throws Exception {
        return testAnalysisCacheFile.getAbsoluteFile();
    }
}

package scala_maven;

import java.io.File;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Compile Scala test source into test-classes.  Corresponds roughly to testCompile
 * in maven-compiler-plugin
 *
 * @phase test-compile
 * @goal testCompile
 * @requiresDependencyResolution test
 * @threadSafe
 */
public class ScalaTestCompileMojo extends ScalaCompilerSupport {

    /**
     * Set this to 'true' to bypass unit tests entirely.
     * Its use is NOT RECOMMENDED, but quite convenient on occasion.
     *
     * @parameter expression="${maven.test.skip}"
     */
    protected boolean skip;

    /**
     * The directory in which to place test compilation output
     *
     * @parameter expression="${project.build.testOutputDirectory}
     */
    protected File testOutputDir;

    /**
     * The directory in which to find test scala source code
     *
     * @parameter expression="${project.build.testSourceDirectory}/../scala"
     */
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
     * @parameter expression="${testAnalysisCacheFile}" default-value="${project.build.directory}/analysis/test-compile"
     */
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

 package scala_maven;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.model.Dependency;

/**
 * Compiles a directory of Scala source. Corresponds roughly to the compile goal
 * of the maven-compiler-plugin
 *
 * @phase compile
 * @goal compile
 * @requiresDependencyResolution compile
 * @threadSafe
 */
public class ScalaCompileMojo extends ScalaCompilerSupport {

    /**
     * The directory in which to place compilation output
     *
     * @parameter expression="${project.build.outputDirectory}"
     */
    protected File outputDir;

    /**
     * The directory which contains scala/java source files
     *
     * @parameter expression="${project.build.sourceDirectory}/../scala"
     */
    protected File sourceDir;

    @Override
    @SuppressWarnings("unchecked")
    protected List<File> getSourceDirectories() throws Exception {
        List<String> sources = project.getCompileSourceRoots();
        String scalaSourceDir = FileUtils.pathOf(sourceDir, useCanonicalPath);
        if(!sources.contains(scalaSourceDir)) {
            sources = new LinkedList<String>(sources); //clone the list to keep the original unmodified
            sources.add(scalaSourceDir);
        }
        return normalize(sources);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<String> getClasspathElements() throws Exception {
        List<String> back = project.getCompileClasspathElements();
        back.remove(project.getBuild().getOutputDirectory());
        back.add(getOutputDir().getAbsolutePath());
        back = TychoUtilities.addOsgiClasspathElements(project, back);
        return back;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<Dependency> getDependencies() {
        return project.getCompileDependencies();
    }

    @Override
    protected File getOutputDir() throws Exception {
        return outputDir.getAbsoluteFile();
    }
}

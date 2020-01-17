package scala_maven;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * Compiles a directory of Scala source. Corresponds roughly to the compile goal
 * of the maven-compiler-plugin
 *
 */
@Mojo(name = "compile", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class ScalaCompileMojo extends ScalaCompilerSupport {

    /**
     * The directory in which to place compilation output
     */
    @Parameter(property = "outputDir", defaultValue = "${project.build.outputDirectory}")
    private File outputDir;

    /**
     * The directory which contains scala/java source files
     */
    @Parameter(defaultValue = "${project.build.sourceDirectory}/../scala")
    private File sourceDir;

    /**
     * Analysis cache file for incremental recompilation.
     *
     */
    @Parameter(property = "analysisCacheFile", defaultValue = "${project.build.directory}/analysis/compile")
    private File analysisCacheFile;

    /**
     * List of directories or jars to add to the classpath
     *
     */
    @Parameter(property = "classpath")
    private Classpath classpath;

    @Override
    protected List<File> getSourceDirectories() throws Exception {
        List<String> sources = project.getCompileSourceRoots();
        String scalaSourceDir = FileUtils.pathOf(sourceDir, useCanonicalPath);
        if (!sources.contains(scalaSourceDir)) {
            sources = new LinkedList<>(sources); // clone the list to keep the original unmodified
            sources.add(scalaSourceDir);
        }
        return normalize(sources);
    }

    @Override
    protected List<String> getClasspathElements() throws Exception {
        final List<String> back = project.getCompileClasspathElements();
        back.remove(project.getBuild().getOutputDirectory());
        // back.add(getOutputDir().getAbsolutePath());
        if (classpath != null && classpath.getAdd() != null) {
            for (File f : classpath.getAdd()) {
                back.add(f.getAbsolutePath());
            }
        }
        back.addAll(TychoUtilities.addOsgiClasspathElements(project));
        return back;
    }

    @Override
    @Deprecated
    protected List<Dependency> getDependencies() {
        return project.getCompileDependencies();
    }

    @Override
    protected File getOutputDir() {
        return outputDir.getAbsoluteFile();
    }

    @Override
    protected File getAnalysisCacheFile() {
        return analysisCacheFile.getAbsoluteFile();
    }
}

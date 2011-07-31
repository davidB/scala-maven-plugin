package org_scala_tools_maven_cs;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.yaml.snakeyaml.Yaml;

/**
 * Register the current project into running ScalaCS. If there is no running ScalaCS then install (if needed) and start it.
 *
 * @goal cs-init
 * @requiresDependencyResolution test
 */
public class ScalaCSInitMojo extends ScalaCSMojoSupport {

    /**
     * The directory in which to place compilation output
     *
     * @parameter expression="${project.build.outputDirectory}"
     */
    protected File outputDir;

    /**
     * A list of inclusion filters for the compiler.
     * ex :
     * <pre>
     *    &lt;includes&gt;
     *      &lt;include&gt;SomeFile.scala&lt;/include&gt;
     *    &lt;/includes&gt;
     * </pre>
     *
     * @parameter
     */
    private Set<String> includes = new HashSet<String>();

    /**
     * A list of exclusion filters for the compiler.
     * ex :
     * <pre>
     *    &lt;excludes&gt;
     *      &lt;exclude&gt;SomeBadFile.scala&lt;/exclude&gt;
     *    &lt;/excludes&gt;
     * </pre>
     *
     * @parameter
     */
    private Set<String> excludes = new HashSet<String>();

    /**
     * The directory which contains scala/java source files
     *
     * @parameter expression="${project.build.sourceDirectory}/../scala"
     */
    protected File sourceDir;

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

    /**
     * The directory in which to find test scala source code
     *
     * @parameter expression="${maven.scalacs.dumpYaml}" default-value="true"
     */
    protected boolean dumpYaml;

    /**
     * Should send a compilation request after initialization.
     *
     * @parameter expression="${maven.scalacs.compileAfterInit}" default-value="false"
     */
    protected boolean compileAfterInit;

    @Override
    protected CharSequence doRequest() throws Exception {
        String yaml = toYaml(project).toString();
        if (dumpYaml) {
            new File(project.getBuild().getDirectory()).mkdirs();
            FileUtils.fileWrite(project.getBuild().getDirectory() + "/project.yaml", "UTF-8", yaml);
        }
        StringBuilder back = new StringBuilder();
        back.append(scs.sendRequestCreateOrUpdate(yaml));
        if (compileAfterInit) {
            back.append(scs.sendRequestCompile(project.getArtifactId()+"-"+project.getVersion(), true, true));
        }
        return back;
    }

    private CharSequence toYaml(MavenProject project) throws Exception {
        HashMap<String, Object> dataCompile = new HashMap<String, Object>();
        /*
        name : sample
        sourceDirs :
          - "/home/dwayne/work/oss/scala-tools/scala-tools-server/src/main/scala"
        includes :
          - "*.scala"
        excludes :
        targetDir : "/home/dwayne/work/oss/scala-tools/scala-tools-server/target/classes"
        classpath :
          - "/home/dwayne/.m2/repository/org/scala-lang/scala-library/2.7.5/scala-library-2.7.5.jar"
          - "/home/dwayne/.m2/repository/org/scala-lang/scala-compiler/2.7.5/scala-compiler-2.7.5.jar"
          - "/home/dwayne/.m2/repository/org/jboss/netty/netty/3.1.0.GA/netty-3.1.0.GA.jar"
          - "/home/dwayne/.m2/repository/SnakeYAML/SnakeYAML/1.3/SnakeYAML-1.3.jar"
        args :
          - "-deprecation"
        */
        dataCompile.put("name", project.getArtifactId()+"-"+project.getVersion()+"/main");
        dataCompile.put("sourceDirs", getSourceDirectories());
        if (includes != null) {
            dataCompile.put("includes", new ArrayList<String>(includes));
        }
        if (excludes != null) {
            dataCompile.put("excludes", new ArrayList<String>(excludes));
        }
        dataCompile.put("targetDir", outputDir.getAbsolutePath());
        dataCompile.put("classpath", project.getCompileClasspathElements());
        if (args != null) {
            dataCompile.put("args", args);
        }
        dataCompile.put("exported", new File(localRepo.getBasedir() , localRepo.pathOf(project.getArtifact())).getAbsolutePath());

        HashMap<String, Object> dataTest = new HashMap<String, Object>();
        dataTest.put("name", project.getArtifactId()+"-"+project.getVersion() +"/test");
        dataTest.put("sourceDirs", project.getTestCompileSourceRoots());
        if (includes != null) {
            dataTest.put("includes", new ArrayList<String>(includes));
        }
        if (excludes != null) {
            dataTest.put("excludes", new ArrayList<String>(excludes));
        }
        dataTest.put("targetDir", testOutputDir.getAbsolutePath());
        dataTest.put("classpath", project.getTestClasspathElements());
        if (args != null) {
            dataTest.put("args", args);
        }

        Yaml yaml = new Yaml();
        List<HashMap<String, Object>> prjs = new LinkedList<HashMap<String, Object>>();
        prjs.add(dataCompile);
        prjs.add(dataTest);
        return yaml.dumpAll(prjs.iterator());
    }

    @SuppressWarnings("unchecked")
    protected List<String> getSourceDirectories() throws Exception {
        List<String> sources = project.getCompileSourceRoots();
        //Quick fix in case the user has not added the "add-source" goal.
        String scalaSourceDir = sourceDir.getAbsolutePath();
        if(!sources.contains(scalaSourceDir)) {
            sources.add(scalaSourceDir);
        }
        return sources;
    }
}

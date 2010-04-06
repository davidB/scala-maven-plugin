package org_scala_tools_maven_cs;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.yaml.snakeyaml.Yaml;

/**
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
    protected void doExecute() throws Exception {
        super.doExecute();
        String yaml = toYaml(project).toString();
        if (dumpYaml) {
            new File(project.getBuild().getDirectory()).mkdirs();
            FileUtils.fileWrite(project.getBuild().getDirectory() + "/project.yaml", "UTF-8", yaml);
        }
        //TODO use parser and maven logger to print (and find warning, error,...)
        System.out.println(scs.sendRequestCreateOrUpdate(yaml));
        if (compileAfterInit) {
            System.out.println(scs.sendRequestCompile(project.getArtifactId()+"-"+project.getVersion(), true, true));
        }
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
        dataCompile.put("targetDir", outputDir.getCanonicalPath());
        dataCompile.put("classpath", project.getCompileClasspathElements());
        if (args != null) {
            dataCompile.put("args", args);
        }
        dataCompile.put("exported", new File(localRepo.getBasedir() , localRepo.pathOf(project.getArtifact())).getCanonicalPath());

        HashMap<String, Object> dataTest = new HashMap<String, Object>();
        dataTest.put("name", project.getArtifactId()+"-"+project.getVersion() +"/test");
        dataTest.put("sourceDirs", project.getTestCompileSourceRoots());
        if (includes != null) {
            dataTest.put("includes", new ArrayList<String>(includes));
        }
        if (excludes != null) {
            dataTest.put("excludes", new ArrayList<String>(excludes));
        }
        dataTest.put("targetDir", testOutputDir.getCanonicalPath());
        dataTest.put("classpath", project.getTestClasspathElements());
        if (args != null) {
            dataTest.put("args", args);
        }

        Yaml yaml = new Yaml();
        StringBuilder back = new StringBuilder();
        back.append(yaml.dump(dataCompile))
//            .append("\n---\n")
            //.append(yaml.dump(dataTest))
            ;
        return back;
    }

    @SuppressWarnings("unchecked")
    protected List<String> getSourceDirectories() throws Exception {
        List<String> sources = project.getCompileSourceRoots();
        //Quick fix in case the user has not added the "add-source" goal.
        String scalaSourceDir = sourceDir.getCanonicalPath();
        if(!sources.contains(scalaSourceDir)) {
            sources.add(scalaSourceDir);
        }
        return sources;
    }
}

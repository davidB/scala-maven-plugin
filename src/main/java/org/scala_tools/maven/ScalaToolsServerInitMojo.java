package org.scala_tools.maven;

import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.scala_tools.maven.executions.JavaMainCaller;
import org.scala_tools.maven.executions.JavaMainCallerByFork;
import org.scala_tools.maven.executions.MainHelper;
import org.yaml.snakeyaml.Yaml;

/**
 * @goal sts-init
 * @requiresDependencyResolution test
 */
public class ScalaToolsServerInitMojo extends ScalaMojoSupport {
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


    @Override
    protected void doExecute() throws Exception {
        System.out.println(sendRequestAdd());
        System.out.println(sendRequestCompile());
    }

    private String toYaml(MavenProject project) throws Exception {
        HashMap<String, Object> data = new HashMap<String, Object>();
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
        data.put("name", project.getArtifactId()+"-"+project.getVersion());
        data.put("sourcesDirs", getSourceDirectories());
        if (includes != null) {
            data.put("includes", new ArrayList<String>(includes));
        }
        if (excludes != null) {
            data.put("excludes", new ArrayList<String>(excludes));
        }
        data.put("targetDir", outputDir.getCanonicalPath());
        data.put("classpath", project.getCompileClasspathElements());
        if (args != null) {
            data.put("args", args);
        }
        Yaml yaml = new Yaml();
        return yaml.dump(data);
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

    protected String sendRequestAdd() throws Exception {
        String yamlDef = toYaml(project);
        String back = "";
        try {
            back = sendRequest("add", yamlDef);
        } catch (java.net.ConnectException exc) {
            startNewServer();
            back = sendRequest("add", yamlDef);
        }
        return back;
    }

    protected String sendRequestCompile() throws Exception {
        return sendRequest("compile", null);
    }

    protected String sendRequestStop() throws Exception {
        return sendRequest("stop", null);
    }

    protected String sendRequest(String action, String data) throws Exception {
        URL url = new URL("http://127.0.0.1:27616/" + action);
        URLConnection cnx = url.openConnection();
        cnx.setDoOutput(StringUtils.isNotEmpty(data));
        cnx.setDoInput(true);
        if (StringUtils.isNotEmpty(data)) {
            IOUtil.copy(data, cnx.getOutputStream());
            IOUtil.close(cnx.getOutputStream());
        }
        String back = IOUtil.toString(cnx.getInputStream());
        IOUtil.close(cnx.getInputStream());
        return back;
    }

    private void startNewServer() throws Exception {
        getLog().info("start scala-tools-server...");
        Set<String> classpath = new HashSet<String>();
        addToClasspath(SCALA_GROUPID, "scala-compiler", scalaVersion, classpath);
        addToClasspath("org.scala-tools", "scala-tools-server", "0.1-SNAPSHOT", classpath);
        JavaMainCaller jcmd = new JavaMainCallerByFork(this, "org.scala_tools.server.HttpServer", MainHelper.toMultiPath(classpath.toArray(new String[classpath.size()])), null, null, forceUseArgFile);
        jcmd.spawn(displayCmd);
        boolean started = false;
        while (!started) {
            try {
                System.out.print(".");
                Thread.sleep(1000);
                sendRequest("ping", null);
                started = true;
                System.out.println("\n started");
            } catch (java.net.ConnectException exc) {
                started = false; //useless but more readable
            }
        }
    }
}

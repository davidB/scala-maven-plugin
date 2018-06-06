package scala_maven;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.strategy.SelfFirstStrategy;
import org.codehaus.plexus.classworlds.strategy.Strategy;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

import scala_maven_executions.JavaMainCaller;
import scala_maven_executions.MainHelper;

/**
 * Run a scala script.
 *
 * @since 2.7
 */
@Mojo(name = "script", requiresDependencyResolution = ResolutionScope.RUNTIME, executionStrategy = "always", threadSafe = true)
public class ScalaScriptMojo extends ScalaMojoSupport {

    /*
     * If the maven-scala-project is a dependency of the project then the
     * MavenProject object describing the project will be passed to the script.
     */

    /**
     * The build directory of the project
     *
     */
    @Parameter(property = "project.build.directory")
    protected File outputDir;

    /**
     * The file containing script to be executed. Either '<em>scriptFile</em>'
     * or '<em>script</em>' must be defined.
     *
     */
    @Parameter(property = "scriptFile")
    protected File scriptFile;

    /**
     * The encoding of file containing script to be executed.
     *
     */
    @Parameter(property = "scriptEncoding", defaultValue = "UTF-8")
    protected String scriptEncoding;

    /**
     * The script that will be executed. Either '<em>scriptFile</em>' or '
     * <em>script</em>' must be defined.
     *
     */
    @Parameter(property = "script")
    protected String script;

    /**
     * If set to true the Scala classfile that is generated will not be deleted
     * after the goal completes. This is to allows easier debugging of the
     * script especially since line numbers will be wrong because lines are
     * added to the compiled script (see script examples)
     *
     */
    @Parameter(property = "maven.scala.keepGeneratedScript", defaultValue = "false")
    protected boolean keepGeneratedScript;

    /**
     * Comma separated list of scopes to add to the classpath.
     * The possible scopes are : test,compile, system, runtime, plugin.
     * By default embedded script into pom.xml run with 'plugin' scope
     * and script read from scriptFile run with 'compile, test, runtime'
     *
     */
    @Parameter(property = "maven.scala.includeScopes")
    protected String includeScopes;

    /**
     * Comma separated list of scopes to remove from the classpath. Eg:
     * test,compile
     *
     */
    @Parameter(property = "maven.scala.excludeScopes")
    protected String excludeScopes;

    /**
     * Comma seperated list of directories or jars to add to the classpath
     *
     */
    @Parameter(property="addToClasspath")
    protected String addToClasspath;

    /**
     * Comma separated list of directories or jars to remove from the classpath.
     * This is useful for resolving conflicts in the classpath. For example, the
     * script uses Ant 1.7 and the compiler dependencies pull in Ant 1.5
     * optional which conflicts and causes a crash
     *
     */
    @Parameter(property="removeFromClasspath")
    protected String removeFromClasspath;

    private static AtomicInteger _lastScriptIndex = new AtomicInteger(0);

    private static String scriptBaseNameOf(File scriptFile, int idx) {
      if (scriptFile == null) {
          return "embeddedScript_" + idx;
      }
      int dot = scriptFile.getName().lastIndexOf('.');
      if (dot == -1) {
          return scriptFile.getName() + "_" + idx;
      }
      return scriptFile.getName().substring(0, dot) + "_" + idx;
    }

    @Override
    protected void doExecute() throws Exception {
        if (script == null && scriptFile == null) {
            throw new MojoFailureException(
                    "Either script or scriptFile must be defined");
        }
        if (script != null && scriptFile != null) {
            throw new MojoFailureException(
                    "Only one of script or scriptFile can be defined");
        }
        if (StringUtils.isEmpty(includeScopes)) {
            if (scriptFile != null) {
                includeScopes = "compile, test, runtime";
            } else {
                includeScopes = "plugin";
            }
        }
        if (excludeScopes == null) {
          excludeScopes = "";
        }
        // prepare
        File scriptDir = new File(outputDir, ".scalaScriptGen");
        scriptDir.mkdirs();
        String baseName = scriptBaseNameOf(scriptFile, _lastScriptIndex.incrementAndGet());
        File destFile = new File(scriptDir, baseName  + ".scala");

        Set<String> classpath = new HashSet<String>();
        configureClasspath(classpath);


        boolean mavenProjectDependency = includeScopes.contains("plugin");
        wrapScript(destFile, mavenProjectDependency);

        try {
            URLClassLoader loader = createScriptClassloader(scriptDir, classpath);
        	getLog().debug(("classpath : " + Arrays.asList(loader.getURLs())));
            compileScript(scriptDir, destFile, loader);
            runScript(mavenProjectDependency, loader, baseName);
        } finally {
            if (!keepGeneratedScript) {
                delete(scriptDir);
            }
        }

    }

    private void runScript(boolean mavenProjectDependency, URLClassLoader loader, String baseName) throws Exception {
        Class<?> compiledScript = loader.loadClass(baseName);

        ClassLoader currentCL = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(loader);
        try {
            try {
                Object instance;
                if (mavenProjectDependency) {
                    Constructor<?> constructor = compiledScript.getConstructor(MavenProject.class, MavenSession.class, Log.class);
                    instance = constructor.newInstance(project, session, getLog());
                } else {
                    instance = compiledScript.newInstance();
                }
                try {
                    compiledScript.getMethod("run").invoke(instance);
                } catch (NoSuchMethodException e) {
                    // ignore because if there is no method then its ok and we
                    // just
                    // don't run the method. initialization of the class must
                    // have been
                    // enough
                }
            } catch (InvocationTargetException e) {
                if (e.getTargetException() != null) {
                    throw e.getTargetException();
                } else if (e.getCause() != null) {
                    throw e.getCause();
                } else {
                    throw e;
                }
            } catch (ExceptionInInitializerError e) {
                if (e.getException() != null) {
                    throw e.getException();
                } else if (e.getCause() != null) {
                    throw e.getCause();
                } else {
                    throw e;
                }
            }
        } catch (Throwable e) {
            if (e instanceof Exception) {
                throw (Exception) e;
            }
            throw new Exception("A " + e.getClass().getSimpleName() + " exception was thrown", e);
        } finally {
            Thread.currentThread().setContextClassLoader(currentCL);
        }
    }

    private URLClassLoader createScriptClassloader(File scriptDir, Set<String> classpath) throws Exception {
        ClassWorld w = new ClassWorld("zero", null);
        w.newRealm("mojo", getClass().getClassLoader());
        Strategy s = new SelfFirstStrategy(w.newRealm("scalaScript", null));
        ClassRealm rScript = s.getRealm();
        rScript.setParentClassLoader(getClass().getClassLoader());
        //rScript.importFrom("mojo", MavenProject.class.getPackage().getName());
        //rScript.importFrom("mojo", MavenSession.class.getPackage().getName());
        //rScript.importFrom("mojo", Log.class.getPackage().getName());
        rScript.importFrom("mojo", "org.apache.maven");
        // add the script directory to the classpath
        rScript.addURL(scriptDir.toURI().toURL());

        for (String string : classpath) {
        	rScript.addURL(new File(string).toURI().toURL());
        }
        return rScript;
    }

    private void compileScript(File scriptDir, File destFile, URLClassLoader loader) throws Exception {
        JavaMainCaller jcmd = getScalaCommand();
        jcmd.addArgs("-classpath", MainHelper.toClasspathString(loader));
        jcmd.addArgs("-d", scriptDir.getAbsolutePath());
        jcmd.addArgs("-sourcepath", scriptDir.getAbsolutePath());
        jcmd.addArgs(destFile.getAbsolutePath());

        jcmd.run(displayCmd);
    }

    private void configureClasspath(Set<String> classpath) throws Exception, DependencyResolutionRequiredException {
        Set<String> includes = new TreeSet<String>(Arrays.asList(StringUtils.split(includeScopes.toLowerCase(),",")));
        Set<String> excludes = new TreeSet<String>(Arrays.asList(StringUtils.split(excludeScopes.toLowerCase(),",")));

        for(Artifact a : project.getArtifacts()) {
          if (includes.contains(a.getScope().toLowerCase()) && !excludes.contains(a.getScope())) {
            addToClasspath(a, classpath, true);
          }
        }

        if (includes.contains("plugin") && !excludes.contains("plugin")) {
            //Plugin plugin = project.getPlugin("scala-maven-plugin");
            for(Plugin p : project.getBuildPlugins()) {
                if ("scala-maven-plugin".equals(p.getArtifactId())) {
                    for(Dependency d : p.getDependencies()) {
                        addToClasspath(factory.createDependencyArtifact(d), classpath, true);
                    }
                }
            }
            for(Artifact a : project.getPluginArtifacts()) {
                if ("scala-maven-plugin".equals(a.getArtifactId())) {
                    addToClasspath(a, classpath, true);
                }
            }
        }

        if (addToClasspath != null) {
            classpath.addAll(Arrays.asList(StringUtils.split(addToClasspath,",")));
        }

        if (removeFromClasspath != null) {
            ArrayList<String> toRemove = new ArrayList<String>();
            String[] jars =  StringUtils.split(removeFromClasspath.trim(),",");
            for (String string : classpath) {
                for (String jar : jars) {
                    if (string.contains(jar.trim())) {
                        toRemove.add(string);
                    }
                }
            }
            classpath.removeAll(toRemove);
        }

//        String outputDirectory = project.getBuild().getOutputDirectory();
//        if(!outputDirectory.endsWith("/")){
//            // need it to end with / for URLClassloader
//            outputDirectory+="/";
//        }
//        classpath.add( outputDirectory);
        addCompilerToClasspath(classpath);
        addLibraryToClasspath(classpath);
        //TODO check that every entry from the classpath exists !
        boolean ok = true;
        for (String s : classpath) {
            File f = new File(s);
            getLog().debug("classpath entry for running and compiling scripts: " + f);
            if (!f.exists()) {
                getLog().error("classpath entry for script not found : " + f);
                ok = false;
            }
        }
        if (!ok) {
            throw new MojoFailureException("some script dependencies not found (see log)");
        }
        getLog().debug("Using the following classpath for running and compiling scripts: "+classpath);

    }

    private void wrapScript(File destFile, boolean mavenProjectDependency) throws IOException {
        destFile.delete();

        FileOutputStream fileOutputStream = new FileOutputStream(destFile);
        PrintStream out = new PrintStream(fileOutputStream, false, encoding);
        BufferedReader reader = null;
        try {
            if (scriptFile != null) {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(scriptFile), Charset.forName(scriptEncoding)));
            } else {
                reader = new BufferedReader(new StringReader(script));
            }

            String baseName = FileUtils.basename(destFile.getName(), ".scala");
            if (mavenProjectDependency) {
//                out.println("import scala.collection.jcl.Conversions._");
                out.println("class " + baseName
                        + "(project :" + MavenProject.class.getCanonicalName()
                        + ",session :" + MavenSession.class.getCanonicalName()
                        + ",log :"+Log.class.getCanonicalName()
                        +") {"
                        );
            } else {
                out.println("class " + baseName + " {");
            }

            String line = reader.readLine();
            while (line != null) {
                out.print("  ");
                out.println(line);
                line = reader.readLine();
            }

            out.println("}");
        } finally {
            IOUtil.close(out);
            IOUtil.close(fileOutputStream);
            IOUtil.close(reader);
        }
    }

    private void delete(File scriptDir) {
        if (scriptDir.isDirectory()) {
            for (File file : scriptDir.listFiles()) {
                delete(file);
            }
        }

        scriptDir.deleteOnExit();
        scriptDir.delete();
    }
}

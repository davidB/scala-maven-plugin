package scala_maven;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.strategy.SelfFirstStrategy;
import org.codehaus.plexus.classworlds.strategy.Strategy;
import org.codehaus.plexus.util.StringUtils;

import scala_maven_executions.JavaMainCaller;
import scala_maven_executions.MainHelper;


/**
 * Run a scala script.
 *
 * @goal script
 * @requiresDependencyResolution runtime
 * @executionStrategy always
 * @since 2.7
 * @threadSafe
 */
public class ScalaScriptMojo extends ScalaMojoSupport {

    /*
     * If the maven-scala-project is a dependency of the project then the
     * MavenProject object describing the project will be passed to the script.
     */

    /**
     * The build directory of the project
     *
     * @parameter expression="${project.build.directory}"
     */
    protected File outputDir;

    /**
     * The file containing script to be executed. Either '<em>scriptFile</em>'
     * or '<em>script</em>' must be defined.
     *
     * @parameter expression="${scriptFile}"
     */
    protected File scriptFile;

    /**
     * The script that will be executed. Either '<em>scriptFile</em>' or '
     * <em>script</em>' must be defined.
     *
     * @parameter expression="${script}"
     */
    protected String script;

    /**
     * If set to true the Scala classfile that is generated will not be deleted
     * after the goal completes. This is to allows easier debugging of the
     * script especially since line numbers will be wrong because lines are
     * added to the compiled script (see script examples)
     *
     * @parameter expression="${maven.scala.keepGeneratedScript}"
     *            default-value="false"
     */
    protected boolean keepGeneratedScript;

    /**
     * Comma separated list of scopes to add to the classpath.
     * The possible scopes are : test,compile, system, runtime, plugin.
     * By default embedded script into pom.xml run with 'plugin' scope
     * and script read from scriptFile run with 'compile, test, runtime'
     *
     * @parameter expression="${maven.scala.includeScopes}"
     */
    protected String includeScopes;

    /**
     * Comma separated list of scopes to remove from the classpath. Eg:
     * test,compile
     *
     * @parameter expression="${maven.scala.excludeScopes}"
     */
    protected String excludeScopes;

    /**
     * Comma seperated list of directories or jars to add to the classpath
     *
     * @parameter expression="${addToClasspath}"
     */
    protected String addToClasspath;

    /**
     * Comma separated list of directories or jars to remove from the classpath.
     * This is useful for resolving conflicts in the classpath. For example, the
     * script uses Ant 1.7 and the compiler dependencies pull in Ant 1.5
     * optional which conflicts and causes a crash
     *
     * @parameter expression="${removeFromClasspath}"
     */
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
                includeScopes= Scopes.PLUGIN.name();
            }
        }

        // prepare
        File scriptDir = new File(outputDir, ".scalaScriptGen");
        scriptDir.mkdirs();
        String baseName = scriptBaseNameOf(scriptFile, _lastScriptIndex.incrementAndGet());
        File destFile = new File(scriptDir, baseName  + ".scala");

        Set<String> classpath = new HashSet<String>();
        configureClasspath(classpath);


        boolean mavenProjectDependency = true;//hasMavenProjectDependency(loader);
        wrapScript(destFile, mavenProjectDependency);

        try {
            compileScript(scriptDir, destFile, classpath);
            URLClassLoader loader = createScriptClassloader(scriptDir, classpath);
            runScript(mavenProjectDependency, loader, baseName);
        } finally {
            if (!keepGeneratedScript) {
                delete(scriptDir);
            }
        }

    }

//    private boolean hasMavenProjectDependency(URLClassLoader loader) throws Exception {
//        try {
//            loader.loadClass(MavenProject.class.getCanonicalName());
//            return true;
//        } catch (ClassNotFoundException e) {
//            return false;
//        }
//    }

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
        ClassRealm rMojo = w.newRealm("mojo", getClass().getClassLoader());
        Strategy s = new SelfFirstStrategy(w.newRealm("scalaScript", null));
        ClassRealm rScript = s.getRealm();
        rScript.setParentClassLoader(getClass().getClassLoader());
        rScript.importFrom("mojo", MavenProject.class.getPackage().getName());
        rScript.importFrom("mojo", MavenSession.class.getPackage().getName());
        rScript.importFrom("mojo", Log.class.getPackage().getName());        
        // add the script directory to the classpath
        rScript.addURL(scriptDir.toURI().toURL());

        for (String string : classpath) {
        	rScript.addURL(new File(string).toURI().toURL());
        }
        return rScript;
    }

    private void compileScript(File scriptDir, File destFile,
            Set<String> classpath) throws Exception {
        JavaMainCaller jcmd = getScalaCommand();
        jcmd.addArgs("-classpath", MainHelper.toMultiPath(new ArrayList<String>(classpath)));
        jcmd.addArgs("-d", scriptDir.getAbsolutePath());
        jcmd.addArgs("-sourcepath", scriptDir.getAbsolutePath());
        jcmd.addArgs(destFile.getAbsolutePath());

        jcmd.run(displayCmd);
    }

    private void configureClasspath(Set<String> classpath) throws Exception, DependencyResolutionRequiredException {
        Collection<Dependency> toInclude = new ArrayList<Dependency>();
        if (includeScopes == null || includeScopes.length() == 0) {
            getLog().warn("No scopes were included");
        } else {

            String[] include = includeScopes.split(",");
            for (String string : include) {
                Scopes scope = Scopes.lookup(string.toUpperCase());
                if (scope != null) {
                    toInclude.addAll(scope.elements(project));
                } else {
                    getLog().warn(
                            "Included Scope: " + string + " is not one of: "
                                    + Arrays.asList(Scopes.values()));
                }
            }
        }
        if (excludeScopes != null && excludeScopes.length() > 0) {

            String[] exclude = excludeScopes.split(",");
            for (String string : exclude) {
                Scopes scope = Scopes.lookup(string.toUpperCase());
                if (scope != null) {
                    toInclude.removeAll(scope.elements(project));
                } else {
                    getLog().warn(
                            "Excluded Scope: " + string + " is not one of: "
                                    + Arrays.asList(Scopes.values()));
                }
            }
        }

        for (Dependency dependency : toInclude) {
            addToClasspath(factory.createDependencyArtifact(dependency
                    .getGroupId(), dependency.getArtifactId(), VersionRange
                    .createFromVersion(dependency.getVersion()), dependency
                    .getType(), dependency.getClassifier(), dependency
                    .getScope(), dependency.isOptional()), classpath, true);
        }



        if (addToClasspath != null) {
            classpath.addAll(Arrays.asList(addToClasspath.split(",")));
        }

        if (removeFromClasspath != null) {
            ArrayList<String> toRemove = new ArrayList<String>();
            String[] jars = removeFromClasspath.trim().split(",");
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
        String sv = findScalaVersion().toString();
        addCompilerToClasspath(sv, classpath);
        addLibraryToClasspath(sv, classpath);
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
        PrintStream out = new PrintStream(fileOutputStream);
        try {
            BufferedReader reader;
            if (scriptFile != null) {
                reader = new BufferedReader(new FileReader(scriptFile));
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
            out.close();
            fileOutputStream.close();
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

    private enum Scopes {
        COMPILE {
            @Override
            public Collection<Dependency> elements(MavenProject project) throws DependencyResolutionRequiredException {
                return project.getCompileDependencies();
            }
        },
        RUNTIME {
            @Override
            public Collection<Dependency> elements(MavenProject project) throws DependencyResolutionRequiredException {
                return project.getRuntimeDependencies();
            }
        },
        TEST {
            @Override
            public Collection<Dependency> elements(MavenProject project) throws DependencyResolutionRequiredException {
                return project.getTestDependencies();
            }
        },
        SYSTEM {
            @Override
            public Collection<Dependency> elements(MavenProject project) throws DependencyResolutionRequiredException {
                return project.getSystemDependencies();
            }
        },
        PLUGIN {
            @Override
            public Collection<Dependency> elements(MavenProject project) throws DependencyResolutionRequiredException {
                Plugin me = project.getBuild().getPluginsAsMap().get("net.alchim31.maven:scala-maven-plugin");
                Set<Dependency> back = new HashSet<Dependency>();
                Dependency dep = new Dependency();
                dep.setArtifactId(me.getArtifactId());
                dep.setGroupId(me.getGroupId());
                dep.setVersion(me.getVersion());
                back.add(dep);
                back.addAll(me.getDependencies());
                return back;
            }
        };

        public abstract Collection<Dependency> elements(MavenProject project) throws DependencyResolutionRequiredException;

        public static Scopes lookup(String name) {
            for (Scopes scope : Scopes.values()) {
                if (scope.name().trim().equalsIgnoreCase(name.trim())) {
                    return scope;
                }
            }
            return null;
        }
    }
}

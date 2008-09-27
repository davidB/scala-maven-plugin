package org.scala_tools.maven;

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

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.scala_tools.maven.model.MavenProjectAdapter;


/**
 * Run a scala script.
 * 
 * @goal script
 * @requiresDependencyResolution runtime
 * @executionStrategy always
 * @since 2.7
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
	 * Comma separated list of scopes to add to the classpath. Eg: test,compile
	 * 
	 * @parameter expression="${maven.scala.includeScopes}"
	 *            default-value="compile, test, runtime"
	 * @required
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

	private static int currentScriptIndex = 0;

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
		currentScriptIndex++;

		// prepare
		File scriptDir = new File(outputDir, ".scalaScriptGen");
		scriptDir.mkdirs();
		File destFile = new File(scriptDir + "/" + scriptBaseName() + ".scala");

		Set<String> classpath = new HashSet<String>();
		configureClasspath(classpath);

		URLClassLoader loader = createScriptClassloader(scriptDir, classpath);

		boolean mavenProjectDependency = hasMavenProjectDependency(classpath);
		wrapScript(destFile, mavenProjectDependency);

		try {
			compileScript(scriptDir, destFile, classpath);
			runScript(mavenProjectDependency, loader);
		} finally {
			if (!keepGeneratedScript) {
				delete(scriptDir);
			}
		}

	}

	private boolean hasMavenProjectDependency(Set<String> classpath)
			throws MalformedURLException {
		try {
			List<URL> urls = new ArrayList<URL>();

			// add the script directory to the classpath
			for (String string : classpath) {
				urls.add(new URL("file://" + string));
			}

			URLClassLoader loader = new URLClassLoader(urls
					.toArray(new URL[urls.size()]));

			loader.loadClass(MavenProjectAdapter.class.getCanonicalName());
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	private void runScript(boolean mavenProjectDependency, URLClassLoader loader)
			throws Exception {
		Class<?> compiledScript = loader.loadClass(scriptBaseName());

		try {
			try {
				Object instance;
				if (mavenProjectDependency) {
					Constructor<?> constructor = compiledScript
							.getConstructor(MavenProjectAdapter.class, Log.class);
					instance = constructor.newInstance(new MavenProjectAdapter(
							project), getLog());
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
			} else {
				throw new Exception("A " + e.getClass().getSimpleName()
						+ " exception was thrown", e);
			}
		}
	}

	private URLClassLoader createScriptClassloader(File scriptDir,
			Set<String> classpath) throws MalformedURLException {
		List<URL> urls = new ArrayList<URL>();

		// add the script directory to the classpath
		urls.add(scriptDir.toURI().toURL());

		for (String string : classpath) {
			urls.add(new URL("file://" + string));
		}

		URLClassLoader loader = new URLClassLoader(urls.toArray(new URL[urls
				.size()]), getClass().getClassLoader());
		return loader;
	}

	private void compileScript(File scriptDir, File destFile,
			Set<String> classpath) throws Exception {
		JavaCommand jcmd = getScalaCommand();
		jcmd.addArgs("-classpath", JavaCommand
				.toMultiPath(new ArrayList<String>(classpath)));
		jcmd.addArgs("-d", scriptDir.getAbsolutePath());
		jcmd.addArgs("-sourcepath", scriptDir.getAbsolutePath());
		jcmd.addArgs(destFile.getAbsolutePath());

		jcmd.run(displayCmd);
	}

	private void configureClasspath(Set<String> classpath) throws Exception,
			DependencyResolutionRequiredException {
		MavenProjectAdapter projectAdapter = new MavenProjectAdapter(project);
	
		Collection<Dependency> toInclude = new ArrayList<Dependency>();
		if (includeScopes == null || includeScopes.length() == 0) {
			getLog().warn("No scopes were included");
		} else {

			String[] include = includeScopes.split(",");
			for (String string : include) {
				Scopes scope = Scopes.lookup(string.toUpperCase());
				if (scope != null) {
					toInclude.addAll(scope.elements(projectAdapter));
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
					toInclude.removeAll(scope.elements(projectAdapter));
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
					.getScope(), dependency.isOptional()), classpath);
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
 		
		String outputDirectory = project.getBuild().getOutputDirectory();
		if(!outputDirectory.endsWith("/")){
		    // need it to end with / for URLClassloader
		    outputDirectory+="/";
		}
        classpath.add( outputDirectory);
		addToClasspath("org.scala-lang", "scala-compiler", scalaVersion,
				classpath);
		addToClasspath("org.scala-lang", "scala-library", scalaVersion,
				classpath);
				
		getLog().debug("Using the following classpath for running and compiling scripts: "+classpath);

	}

	private void wrapScript(File destFile, boolean mavenProjectDependency)
			throws IOException {
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

			if (mavenProjectDependency) {
				out.println("import scala.collection.jcl.Conversions._");
				out.println("class " + scriptBaseName() + "(project:"
						+ MavenProjectAdapter.class.getCanonicalName() + ",log:"+Log.class.getCanonicalName()+") {");
			} else {
				out.println("class " + scriptBaseName() + " {");
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

	private String scriptBaseName() {
		if (scriptFile == null) {
			return "embeddedScript_" + currentScriptIndex;
		} else {
			int dot = scriptFile.getName().lastIndexOf('.');
			if (dot == -1) {
				return scriptFile.getName() + "_" + currentScriptIndex;
			}
			return scriptFile.getName().substring(0, dot) + "_"
					+ currentScriptIndex;
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
			public Collection<Dependency> elements(MavenProjectAdapter project)
					throws DependencyResolutionRequiredException {
				return project.getCompileDependencies();
			}
		},
		RUNTIME {
			public Collection<Dependency> elements(MavenProjectAdapter project)
					throws DependencyResolutionRequiredException {
				return project.getRuntimeDependencies();
			}
		},
		TEST {
			public Collection<Dependency> elements(MavenProjectAdapter project)
					throws DependencyResolutionRequiredException {
				return project.getTestDependencies();
			}
		},
		SYSTEM {
			public Collection<Dependency> elements(MavenProjectAdapter project)
					throws DependencyResolutionRequiredException {
				return project.getSystemDependencies();
			}
		};

		public abstract Collection<Dependency> elements(
				MavenProjectAdapter project)
				throws DependencyResolutionRequiredException;

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

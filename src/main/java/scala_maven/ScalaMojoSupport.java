
/*
 * Copyright 2011-2020 scala-maven-plugin project (https://davidb.github.io/scala-maven-plugin/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package scala_maven;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.filter.AncestorOrSelfDependencyNodeFilter;
import org.apache.maven.shared.dependency.graph.filter.AndDependencyNodeFilter;
import org.apache.maven.shared.dependency.graph.filter.DependencyNodeFilter;
import org.apache.maven.shared.dependency.graph.traversal.CollectingDependencyNodeVisitor;
import org.apache.maven.shared.dependency.graph.traversal.DependencyNodeVisitor;
import org.apache.maven.shared.dependency.graph.traversal.FilteringDependencyNodeVisitor;
import org.apache.maven.toolchain.Toolchain;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.util.StringUtils;
import scala_maven_dependency.*;
import scala_maven_executions.JavaMainCaller;
import scala_maven_executions.JavaMainCallerByFork;
import scala_maven_executions.JavaMainCallerInProcess;
import util.FileUtils;

public abstract class ScalaMojoSupport extends AbstractMojo {

  /** The maven project. */
  @Parameter(property = "project", required = true, readonly = true)
  protected MavenProject project;

  /**
   * The Maven Session Object
   *
   * <p>Note: Allows extending for 3rd-party usages
   */
  @Parameter(property = "session", required = true, readonly = true)
  protected MavenSession session;

  /** Used to look up Artifacts in the remote repository. */
  @Component RepositorySystem factory;

  /**
   * Additional dependencies/jar to add to classpath to run "scalaClassName" (scope and optional
   * field not supported) ex :
   *
   * <pre>
   *    &lt;dependencies&gt;
   *      &lt;dependency&gt;
   *        &lt;groupId&gt;org.scala-tools&lt;/groupId&gt;
   *        &lt;artifactId&gt;scala-compiler-addon&lt;/artifactId&gt;
   *        &lt;version&gt;1.0-SNAPSHOT&lt;/version&gt;
   *      &lt;/dependency&gt;
   *    &lt;/dependencies&gt;
   * </pre>
   */
  @Parameter protected BasicArtifact[] dependencies;

  /**
   * Compiler plugin dependencies to use when compiling. ex:
   *
   * <pre>
   * &lt;compilerPlugins&gt;
   *   &lt;compilerPlugin&gt;
   *     &lt;groupId&gt;my.scala.plugin&lt;/groupId&gt;
   *     &lt;artifactId&gt;amazingPlugin&lt;/artifactId&gt;
   *     &lt;version&gt;1.0-SNAPSHOT&lt;/version&gt;
   *   &lt;/compilerPlugin&gt;
   * &lt;/compilerPlugins&gt;
   * </pre>
   */
  @Parameter private BasicArtifact[] compilerPlugins;

  /** Jvm Arguments. */
  @Parameter protected String[] jvmArgs;

  /** compiler additional arguments */
  @Parameter protected String[] args;

  /**
   * Additional parameter to use to call the main class. Use this parameter only from command line
   * ("-DaddScalacArgs=arg1|arg2|arg3|..."), not from pom.xml. To define compiler arguments in
   * pom.xml see the "args" parameter.
   */
  @Parameter(property = "addScalacArgs")
  private String addScalacArgs;

  /** className (FQN) of the scala tool to provide as */
  @Parameter(
      required = true,
      property = "maven.scala.className",
      defaultValue = "scala.tools.nsc.Main")
  protected String scalaClassName;

  /** Scala 's version to use. (property 'maven.scala.version' replaced by 'scala.version') */
  @Parameter(property = "scala.version")
  private String scalaVersion;

  /**
   * Organization/group ID of the Scala used in the project. Default value is 'org.scala-lang'. This
   * is an advanced setting used for clones of the Scala Language. It should be disregarded in
   * standard use cases.
   */
  @Parameter(property = "scala.organization", defaultValue = "org.scala-lang")
  private String scalaOrganization;

  /**
   * Scala 's version to use to check binary compatibility (like suffix in artifactId of
   * dependency). If it is defined then it is used to checkMultipleScalaVersions
   */
  @Parameter(property = "scala.compat.version")
  private String scalaCompatVersion;

  /** Path to Scala installation to use instead of the artifact (define as dependencies). */
  @Parameter(property = "scala.home")
  private String scalaHome;

  /** Arguments for javac (when using incremental compiler). */
  @Parameter(property = "javacArgs")
  protected String[] javacArgs;

  /**
   * Whether to instruct javac to generate debug symbols (when using incremental compiler)
   *
   * @see <a href=
   *     "http://maven.apache.org/plugins/maven-compiler-plugin/compile-mojo.html#debug">://maven.apache.org/plugins/maven-compiler-plugin/compile-mojo.html#debug</a>
   */
  @Parameter(property = "javacGenerateDebugSymbols", defaultValue = "true")
  protected boolean javacGenerateDebugSymbols = true;

  /**
   * Alternative method for specifying javac arguments (when using incremental compiler). Can be
   * used from command line with -DaddJavacArgs=arg1|arg2|arg3|... rather than in pom.xml.
   */
  @Parameter(property = "addJavacArgs")
  protected String addJavacArgs;

  /** The -source argument for the Java compiler (when using incremental compiler). */
  @Parameter(property = "maven.compiler.source")
  protected String source;

  /** The -target argument for the Java compiler (when using incremental compiler). */
  @Parameter(property = "maven.compiler.target")
  protected String target;

  /** The -encoding argument for the Java compiler. (when using incremental compiler). */
  @Parameter(property = "project.build.sourceEncoding", defaultValue = "UTF-8")
  protected String encoding;

  /**
   * Display the command line called ? (property 'maven.scala.displayCmd' replaced by 'displayCmd')
   */
  @Parameter(property = "displayCmd", defaultValue = "false", required = true)
  public boolean displayCmd;

  /** Forks the execution of scalac into a separate process. */
  @Parameter(defaultValue = "true")
  protected boolean fork = true;

  /** Force the use of an external ArgFile to run any forked process. */
  @Parameter(defaultValue = "false")
  protected boolean forceUseArgFile = false;

  /** Check if every dependencies use the same version of scala-library or scala.compat.version. */
  @Parameter(property = "maven.scala.checkConsistency", defaultValue = "true")
  protected boolean checkMultipleScalaVersions;

  /**
   * Determines if a detection of multiple scala versions in the dependencies will cause the build
   * to fail.
   */
  @Parameter(defaultValue = "false")
  protected boolean failOnMultipleScalaVersions = false;

  /**
   * Should use CanonicalPath to normalize path (true =&gt; getCanonicalPath, false =&gt;
   * getAbsolutePath)
   *
   * @see <a href=
   *     "https://github.com/davidB/scala-maven-plugin/issues/50">https://github.com/davidB/scala-maven-plugin/issues/50</a>
   */
  @Parameter(property = "maven.scala.useCanonicalPath", defaultValue = "true")
  protected boolean useCanonicalPath = true;

  /** The dependency tree builder to use. */
  @Component private DependencyGraphBuilder dependencyGraphBuilder;

  /** The toolchain manager to use. */
  @Component protected ToolchainManager toolchainManager;

  /** List of artifacts to run plugin */
  @Parameter(defaultValue = "${plugin.artifacts}")
  private List<Artifact> pluginArtifacts;

  private MavenArtifactResolver mavenArtifactResolver;

  public MavenArtifactResolver findMavenArtifactResolver() {
    if (mavenArtifactResolver == null) {
      mavenArtifactResolver = new MavenArtifactResolver(factory, session);
    }
    return mavenArtifactResolver;
  }

  private Context scalaContext;

  public Context findScalaContext() throws Exception {
    // reuse/lazy scalaContext creation (doesn't need to be Thread safe, scalaContext should be
    // stateless)
    if (scalaContext == null) {
      VersionNumber scalaVersion = findScalaVersion();

      ArtifactIds aids =
          (scalaVersion.major == 3)
              ? new ArtifactIds4Scala3(scalaVersion)
              : new ArtifactIds4Scala2();
      VersionNumber requiredScalaVersion =
          StringUtils.isNotEmpty(scalaCompatVersion)
              ? new VersionNumberMask(scalaCompatVersion)
              : scalaVersion;
      if (requiredScalaVersion.compareTo(scalaVersion) != 0) {
        String msg =
            String.format(
                "Scala library detected %s doesn't match scala.compat.version : %s",
                scalaVersion, requiredScalaVersion);
        if (failOnMultipleScalaVersions) {
          getLog().error(msg);
          throw new MojoFailureException(msg);
        }
        getLog().warn(msg);
      }
      scalaContext =
          StringUtils.isNotEmpty(scalaHome)
              ? new Context4ScalaHome(scalaVersion, requiredScalaVersion, aids, new File(scalaHome))
              : new Context4ScalaRemote(
                  scalaVersion,
                  requiredScalaVersion,
                  aids,
                  scalaOrganization,
                  findMavenArtifactResolver());
    }
    return scalaContext;
  }

  protected void addToClasspath(
      String groupId,
      String artifactId,
      String version,
      String classifier,
      Set<File> classpath,
      boolean addDependencies)
      throws Exception {
    MavenArtifactResolver mar = findMavenArtifactResolver();
    if (addDependencies) {
      for (Artifact a : mar.getJarAndDependencies(groupId, artifactId, version, classifier)) {
        classpath.add(a.getFile());
      }
    } else {
      Artifact a = mar.getJar(groupId, artifactId, version, classifier);
      classpath.add(a.getFile());
    }
  }

  void addCompilerToClasspath(Set<File> classpath) throws Exception {
    Context sc = findScalaContext();
    for (Artifact dep : sc.findCompilerAndDependencies()) {
      classpath.add(dep.getFile());
    }
  }

  void addLibraryToClasspath(Set<File> classpath) throws Exception {
    Context sc = findScalaContext();
    classpath.add(sc.findLibraryJar());
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    try {
      String oldWay = System.getProperty("maven.scala.version");
      if (oldWay != null) {
        getLog().warn("using 'maven.scala.version' is deprecated, use 'scala.version' instead");
        if (scalaVersion != null) {
          scalaVersion = oldWay;
        }
      }

      oldWay = System.getProperty("maven.scala.displayCmd");
      if (oldWay != null) {
        getLog().warn("using 'maven.scala.displayCmd' is deprecated, use 'displayCmd' instead");
        displayCmd = displayCmd || Boolean.parseBoolean(oldWay);
      }
      checkScalaVersion();
      doExecute();
    } catch (MojoExecutionException exc) {
      throw exc;
    } catch (MojoFailureException | RuntimeException exc) {
      throw exc;
    } catch (Exception exc) {
      throw new MojoExecutionException("wrap: " + exc, exc);
    }
  }

  protected List<Dependency> getDependencies() {
    return project.getCompileDependencies();
  }

  private VersionNumber findScalaVersion() throws Exception {
    String detectedScalaVersion = scalaVersion;
    if (StringUtils.isEmpty(detectedScalaVersion)) {
      detectedScalaVersion =
          findVersionFromDependencies(scalaOrganization, ArtifactIds.SCALA_LIBRARY_PATTERN);
    }
    if (StringUtils.isEmpty(detectedScalaVersion)) {
      if (!MavenArtifactResolver.POM.equals(project.getPackaging())) {
        String error =
            String.format(
                "%s:%s is missing from project dependencies",
                scalaOrganization, ArtifactIds.SCALA_LIBRARY_PATTERN.pattern());
        getLog().error(error);
        throw new UnsupportedOperationException(error);
      }
    } else {
      // grappy hack to retrieve the SNAPSHOT version without timestamp,...
      // because if version is -SNAPSHOT and artifact is deploy with uniqueValue then
      // the version
      // get from dependency is with the timestamp and a build number (the resolved
      // version)
      // but scala-compiler with the same version could have different resolved
      // version (timestamp,...)
      boolean isSnapshot = ArtifactUtils.isSnapshot(detectedScalaVersion);
      if (isSnapshot && !detectedScalaVersion.endsWith("-SNAPSHOT")) {
        detectedScalaVersion =
            detectedScalaVersion.substring(
                    0,
                    detectedScalaVersion.lastIndexOf(
                        '-', detectedScalaVersion.lastIndexOf('-') - 1))
                + "-SNAPSHOT";
      }
    }
    if (StringUtils.isEmpty(detectedScalaVersion)) {
      throw new MojoFailureException("no scalaVersion detected or set");
    }
    if (StringUtils.isNotEmpty(scalaVersion)) {
      if (!scalaVersion.equals(detectedScalaVersion)) {
        getLog()
            .warn(
                "scala library version define in dependencies doesn't match the scalaVersion of the plugin");
      }
      // getLog().info("suggestion: remove the scalaVersion from pom.xml");
      // //scalaVersion could be define in a parent pom where lib is not required
    }
    return new VersionNumber(detectedScalaVersion);
  }

  // TODO refactor to do only one scan of dependencies to find version
  private String findVersionFromDependencies(String groupId, Pattern artifactId) {
    String version = null;
    for (Dependency dep : getDependencies()) {
      if (groupId.equals(dep.getGroupId()) && artifactId.matcher(dep.getArtifactId()).find()) {
        version = dep.getVersion();
      }
    }
    if (StringUtils.isEmpty(version)) {
      List<Dependency> deps = new ArrayList<>();
      deps.addAll(project.getModel().getDependencies());
      if (project.getModel().getDependencyManagement() != null) {
        deps.addAll(project.getModel().getDependencyManagement().getDependencies());
      }
      for (Dependency dep : deps) {
        if (groupId.equals(dep.getGroupId()) && artifactId.matcher(dep.getArtifactId()).find()) {
          version = dep.getVersion();
        }
      }
    }
    return version;
  }

  void checkScalaVersion() throws Exception {
    String sv = findScalaVersion().toString();
    if (StringUtils.isNotEmpty(scalaHome)) {
      getLog()
          .warn(
              String.format(
                  "local scala-library.jar and scala-compiler.jar from scalaHome(%s) used instead of scala %s",
                  scalaHome, sv));
    }
    if (checkMultipleScalaVersions) {
      checkCorrectVersionsOfScalaLibrary(sv);
    }
  }

  /**
   * this method checks to see if there are multiple versions of the scala library
   *
   * @throws Exception
   */
  private void checkCorrectVersionsOfScalaLibrary(String scalaDefVersion) throws Exception {
    getLog().debug("Checking for multiple versions of scala");
    // TODO - Make sure we handle bad artifacts....
    // TODO: note that filter does not get applied due to MNG-3236
    VersionNumber sv = new VersionNumber(scalaDefVersion);
    VersionNumber requiredScalaVersion =
        StringUtils.isNotEmpty(scalaCompatVersion) ? new VersionNumberMask(scalaCompatVersion) : sv;
    if (requiredScalaVersion.compareTo(sv) != 0) {
      String msg =
          String.format(
              "Scala library detected %s doesn't match scala.compat.version : %s",
              sv, requiredScalaVersion);
      if (failOnMultipleScalaVersions) {
        getLog().error(msg);
        throw new MojoFailureException(msg);
      }
      getLog().warn(msg);
    }
    ProjectBuildingRequest request =
        new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
    request.setProject(project);
    checkArtifactForScalaVersion(
        findScalaContext(), dependencyGraphBuilder.buildDependencyGraph(request, null));
  }

  /** Visits a node (and all dependencies) to see if it contains duplicate scala versions */
  private void checkArtifactForScalaVersion(Context scalaContext, DependencyNode rootNode)
      throws Exception {
    final CheckScalaVersionVisitor visitor = new CheckScalaVersionVisitor(scalaContext, getLog());

    CollectingDependencyNodeVisitor collectingVisitor = new CollectingDependencyNodeVisitor();
    DependencyNodeVisitor firstPassVisitor =
        new FilteringDependencyNodeVisitor(collectingVisitor, createScalaDistroDependencyFilter());
    rootNode.accept(firstPassVisitor);

    DependencyNodeFilter secondPassFilter =
        new AncestorOrSelfDependencyNodeFilter(collectingVisitor.getNodes());
    DependencyNodeVisitor filteredVisitor =
        new FilteringDependencyNodeVisitor(visitor, secondPassFilter);

    rootNode.accept(filteredVisitor);

    if (visitor.isFailed()) {
      visitor.logScalaDependents();
      if (failOnMultipleScalaVersions) {
        getLog().error("Multiple versions of scala libraries detected!");
        throw new MojoFailureException("Multiple versions of scala libraries detected!");
      }
      getLog().warn("Multiple versions of scala libraries detected!");
    }
  }

  /** @return A filter to only extract artifacts deployed from scala distributions */
  private DependencyNodeFilter createScalaDistroDependencyFilter() throws Exception {
    List<DependencyNodeFilter> filters = new ArrayList<>();
    filters.add(new ScalaDistroArtifactFilter(findScalaContext()));
    return new AndDependencyNodeFilter(filters);
  }

  protected abstract void doExecute() throws Exception;

  protected JavaMainCaller getScalaCommand() throws Exception {
    return getScalaCommand(fork, scalaClassName);
  }

  /**
   * Get a {@link JavaMainCaller} used invoke a Java process. Typically this will be one of the
   * Scala utilities (Compiler, ScalaDoc, REPL, etc.).
   *
   * <p>This method does some setup on the {@link JavaMainCaller} which is not done by merely
   * invoking {@code new} on one of the implementations. Specifically, it adds any Scala compiler
   * plugin options, JVM options, and Scalac options defined on the plugin.
   *
   * @param forkOverride override the setting for {@link #fork}. Currently this should only be set
   *     if you are invoking the REPL.
   * @param mainClass the JVM main class to invoke.
   * @return a {@link JavaMainCaller} to use to invoke the given command.
   */
  final JavaMainCaller getScalaCommand(final boolean forkOverride, final String mainClass)
      throws Exception {
    JavaMainCaller cmd = getEmptyScalaCommand(mainClass, forkOverride);
    cmd.addArgs(args);
    if (StringUtils.isNotEmpty(addScalacArgs)) {
      cmd.addArgs(StringUtils.split(addScalacArgs, "|"));
    }
    addCompilerPluginOptions(cmd);
    cmd.addJvmArgs(jvmArgs);
    return cmd;
  }

  /**
   * Get a {@link JavaMainCaller} used invoke a Java process. Typically this will be one of the
   * Scala utilities (Compiler, ScalaDoc, REPL, etc.).
   *
   * @param mainClass the JVM main class to invoke.
   * @return a {@link JavaMainCaller} to use to invoke the given command.
   */
  final JavaMainCaller getEmptyScalaCommand(final String mainClass) throws Exception {
    return getEmptyScalaCommand(mainClass, fork);
  }

  /**
   * Get a {@link JavaMainCaller} used invoke a Java process. Typically this will be one of the
   * Scala utilities (Compiler, ScalaDoc, REPL, etc.).
   *
   * @param mainClass the JVM main class to invoke.
   * @param forkOverride override the setting for {@link #fork}. Currently this should only be set
   *     if you are invoking the REPL.
   * @return a {@link JavaMainCaller} to use to invoke the given command.
   */
  private JavaMainCaller getEmptyScalaCommand(final String mainClass, final boolean forkOverride)
      throws Exception {

    // If we are deviating from the plugin settings, let the user know
    // what's going on.
    if (forkOverride != fork) {
      super.getLog().info("Fork behavior overridden");
      super.getLog()
          .info(String.format("Fork for this execution is %s.", String.valueOf(forkOverride)));
    }

    // TODO - Fork or not depending on configuration?
    JavaMainCaller cmd;
    String toolcp = getToolClasspath();
    if (forkOverride) {
      // HACK (better may need refactor)
      boolean bootcp = true;
      if (args != null) {
        for (String arg : args) {
          bootcp = bootcp && !"-nobootcp".equals(arg);
        }
      }
      String cp = bootcp ? "" : toolcp;
      bootcp =
          bootcp && !(StringUtils.isNotEmpty(addScalacArgs) && addScalacArgs.contains("-nobootcp"));
      // scalac with args in files
      // * works only since 2.8.0
      // * is buggy (don't manage space in path on windows)
      getLog().debug("use java command with args in file forced : " + forceUseArgFile);
      cmd =
          new JavaMainCallerByFork(
              this, mainClass, cp, null, null, forceUseArgFile, getToolchain());
      if (bootcp) {
        cmd.addJvmArgs("-Xbootclasspath/a:" + toolcp);
      }
    } else {
      cmd = new JavaMainCallerInProcess(this, mainClass, toolcp, null, null);
    }
    return cmd;
  }

  protected Toolchain getToolchain() {
    return toolchainManager.getToolchainFromBuildContext("jdk", session);
  }

  private String getToolClasspath() throws Exception {
    Set<File> classpath = new LinkedHashSet<>();
    addLibraryToClasspath(classpath);
    addCompilerToClasspath(classpath);
    if (dependencies != null) {
      for (BasicArtifact artifact : dependencies) {
        addToClasspath(
            artifact.groupId, artifact.artifactId, artifact.version, "", classpath, true);
      }
    }
    return FileUtils.toMultiPath(classpath);
  }

  protected List<String> getScalaOptions() throws Exception {
    List<String> options = new ArrayList<>();
    if (args != null) Collections.addAll(options, args);
    if (StringUtils.isNotEmpty(addScalacArgs)) {
      Collections.addAll(options, StringUtils.split(addScalacArgs, "|"));
    }
    options.addAll(getCompilerPluginOptions());
    return options;
  }

  protected List<String> getJavacOptions() {
    List<String> options = new ArrayList<>();
    if (javacArgs != null) Collections.addAll(options, javacArgs);
    if (StringUtils.isNotEmpty(addJavacArgs)) {
      Collections.addAll(options, StringUtils.split(addJavacArgs, "|"));
    }

    // issue #116
    if (javacGenerateDebugSymbols) {
      options.add("-g");
    }
    if (target != null && !target.isEmpty()) {
      options.add("-target");
      options.add(target);
    }
    if (source != null && !source.isEmpty()) {
      options.add("-source");
      options.add(source);
    }
    if (encoding != null) {
      options.add("-encoding");
      options.add(encoding);
    }
    return options;
  }

  /**
   * @return This returns whether or not the scala version can support having java sent into the
   *     compiler
   */
  protected boolean isJavaSupportedByCompiler() throws Exception {
    return findScalaVersion().compareTo(new VersionNumber("2.7.2")) >= 0;
  }

  /**
   * Adds appropriate compiler plugins to the scalac command.
   *
   * @param scalac
   * @throws Exception
   */
  protected void addCompilerPluginOptions(JavaMainCaller scalac) throws Exception {
    for (String option : getCompilerPluginOptions()) {
      scalac.addArgs(option);
    }
  }

  private List<String> getCompilerPluginOptions() throws Exception {
    List<String> options = new ArrayList<>();
    for (File plugin : getCompilerPlugins()) {
      options.add("-Xplugin:" + plugin.getPath());
    }
    return options;
  }

  /**
   * Retrieves a list of paths to scala compiler plugins.
   *
   * @return The list of plugins
   * @throws Exception
   */
  private Set<File> getCompilerPlugins() throws Exception {
    Set<File> plugins = new HashSet<>();
    if (compilerPlugins != null) {
      Set<File> ignoreClasspath = new LinkedHashSet<>();
      addCompilerToClasspath(ignoreClasspath);
      addLibraryToClasspath(ignoreClasspath);
      for (BasicArtifact artifact : compilerPlugins) {
        getLog().info("compiler plugin: " + artifact.toString());
        // TODO - Ensure proper scala version for plugins
        Set<File> pluginClassPath = new HashSet<>();
        addToClasspath(
            artifact.groupId,
            artifact.artifactId,
            artifact.version,
            artifact.classifier,
            pluginClassPath,
            false);
        pluginClassPath.removeAll(ignoreClasspath);
        plugins.addAll(pluginClassPath);
      }
    }
    return plugins;
  }
}

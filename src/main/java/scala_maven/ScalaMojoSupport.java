package scala_maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.filter.AncestorOrSelfDependencyNodeFilter;
import org.apache.maven.shared.dependency.tree.filter.AndDependencyNodeFilter;
import org.apache.maven.shared.dependency.tree.filter.DependencyNodeFilter;
import org.apache.maven.shared.dependency.tree.traversal.CollectingDependencyNodeVisitor;
import org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor;
import org.apache.maven.shared.dependency.tree.traversal.FilteringDependencyNodeVisitor;
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.util.StringUtils;
import scala_maven_dependency.CheckScalaVersionVisitor;
import scala_maven_dependency.ScalaDistroArtifactFilter;
import scala_maven_executions.JavaMainCaller;
import scala_maven_executions.JavaMainCallerByFork;
import scala_maven_executions.JavaMainCallerInProcess;
import scala_maven_executions.MainHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public abstract class ScalaMojoSupport extends AbstractMojo {

    public static final String SCALA_LIBRARY_ARTIFACTID= "scala-library";
    public static final String SCALA_COMPILER_ARTIFACTID= "scala-compiler";

    /**
     * @parameter property="project"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * The Maven Session Object
     *
     * @parameter property="session"
     * @required
     * @readonly
     */
    protected MavenSession session;

    /**
     * Contains the full list of projects in the reactor.
     *
     * @parameter default-value="${reactorProjects}"
     * @required
     * @readonly
     */
    protected List<MavenProject> reactorProjects;

    /**
     * Used to look up Artifacts in the remote repository.
     *
     * @component
     * @required
     * @readonly
     */
    protected RepositorySystem factory;

    /**
     * Used to look up Artifacts in the remote repository.
     *
     * @component
     * @required
     * @readonly
     */
    protected ArtifactResolver resolver;
    /**
     * Location of the local repository.
     *
     * @parameter property="localRepository"
     * @readonly
     * @required
     */
    protected ArtifactRepository localRepo;

    /**
     * List of Remote Repositories used by the resolver
     *
     * @parameter property="project.remoteArtifactRepositories"
     * @readonly
     * @required
     */
    protected List<ArtifactRepository> remoteRepos;

    /**
     * Additional dependencies/jar to add to classpath to run "scalaClassName" (scope and optional field not supported)
     * ex :
     * <pre>
     *    &lt;dependencies>
     *      &lt;dependency>
     *        &lt;groupId>org.scala-tools&lt;/groupId>
     *        &lt;artifactId>scala-compiler-addon&lt;/artifactId>
     *        &lt;version>1.0-SNAPSHOT&lt;/version>
     *      &lt;/dependency>
     *    &lt;/dependencies>
     * </pre>
     * @parameter
     */
    protected BasicArtifact[] dependencies;

    /**
     * Compiler plugin dependencies to use when compiling.
     * ex:
     * @parameter
     * <xmp>
     * <compilerPlugins>
     * <compilerPlugin>
     * <groupId>my.scala.plugin</groupId>
     * <artifactId>amazingPlugin</artifactId>
     * <version>1.0-SNAPSHOT</version>
     * </compilerPlugin>
     * </compilerPlugins>
     * </xmp>
     */
    protected BasicArtifact[] compilerPlugins;

    /**
     * Jvm Arguments.
     *
     * @parameter
     */
    protected String[] jvmArgs;

    /**
     * compiler additionnals arguments
     *
     * @parameter
     */
    protected String[] args;

    /**
     * Additional parameter to use to call the main class
     * Using this parameter only from command line ("-DaddScalacArgs=arg1|arg2|arg3|..."), not from pom.xml.
     * @parameter property="addScalacArgs"
     */
    protected String addScalacArgs;

    /**
     * className (FQN) of the scala tool to provide as
     *
     * @required
     * @parameter property="maven.scala.className"
     *            default-value="scala.tools.nsc.Main"
     */
    protected String scalaClassName;

    /**
     * Scala 's version to use.
     * (property 'maven.scala.version' replaced by 'scala.version')
     *
     * @parameter property="scala.version"
     */
    private String scalaVersion;

    /**
     * Organization/group ID of the Scala used in the project.
     * Default value is 'org.scala-lang'.
     * This is an advanced setting used for clones of the Scala Language.
     * It should be disregarded in standard use cases.
     *
     * @parameter property="scala.organization"
     *            default-value="org.scala-lang"
     */
    private String scalaOrganization;

    public String getScalaOrganization(){
        return scalaOrganization;
    }

    /**
     * Scala 's version to use to check binary compatibility (like suffix in artifactId of dependency).
     * If it is defined then it is used to checkMultipleScalaVersions
     *
     * @parameter property="scala.compat.version"
     */
    private String scalaCompatVersion;

    /**
     * Path to Scala installation to use instead of the artifact (define as dependencies).
     *
     * @parameter property="scala.home"
     */
    private String scalaHome;

    /**
     * Arguments for javac (when using incremental compiler).
     *
     * @parameter property="javacArgs"
     */
    protected String[] javacArgs;

    /**
     * Whether to instruct javac to generate debug symbols (when using incremental compiler)
     * @see <a href="http://maven.apache.org/plugins/maven-compiler-plugin/compile-mojo.html#debug">://maven.apache.org/plugins/maven-compiler-plugin/compile-mojo.html#debug</a>
     *
     * @parameter property="javacGenerateDebugSymbols"
     *            default-value="true"
     */
    @SuppressWarnings("unused")
    protected boolean javacGenerateDebugSymbols = true;

    /**
     * Alternative method for specifying javac arguments (when using incremental compiler).
     * Can be used from command line with -DaddJavacArgs=arg1|arg2|arg3|... rather than in pom.xml.
     *
     * @parameter property="addJavacArgs"
     */
    protected String addJavacArgs;


    /**
     * The -source argument for the Java compiler (when using incremental compiler).
     *
     * @parameter property="maven.compiler.source"
     */
    protected String source;

    /**
     * The -target argument for the Java compiler (when using incremental compiler).
     *
     * @parameter property="maven.compiler.target"
     */
    protected String target;

    /**
     * The -encoding argument for the Java compiler. (when using incremental compiler).
     *
     * @parameter property="project.build.sourceEncoding" default-value="UTF-8"
     */
    protected String encoding;

    /**
     * Display the command line called ?
     * (property 'maven.scala.displayCmd' replaced by 'displayCmd')
     *
     * @required
     * @parameter property="displayCmd"
     *            default-value="false"
     */
    public boolean displayCmd;

    /**
     * Forks the execution of scalac into a separate process.
     *
     * @parameter default-value="true"
     */
    protected boolean fork = true;

    /**
     * Force the use of an external ArgFile to run any forked process.
     *
     * @parameter default-value="false"
     */
    protected boolean forceUseArgFile = false;

    /**
     * Check if every dependencies use the same version of scala-library or scala.compat.version.
     *
     * @parameter property="maven.scala.checkConsistency" default-value="true"
     */
    protected boolean checkMultipleScalaVersions;

    /**
     * Determines if a detection of multiple scala versions in the dependencies will cause the build to fail.
     *
     * @parameter default-value="false"
     */
    protected boolean failOnMultipleScalaVersions = false;

    /**
     * Should use CanonicalPath to normalize path (true => getCanonicalPath, false => getAbsolutePath)
     * @see <a href="https://github.com/davidB/maven-scala-plugin/issues/50">https://github.com/davidB/maven-scala-plugin/issues/50</a>
     * @parameter property="maven.scala.useCanonicalPath" default-value="true"
     */
    protected boolean useCanonicalPath = true;

    /**
     * Artifact factory, needed to download source jars.
     *
     * @component
     * @required
     * @readonly
     */
    protected MavenProjectBuilder mavenProjectBuilder;

    /**
     * The artifact repository to use.
     *
     * @parameter property="localRepository"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * The artifact factory to use.
     *
     * @component
     * @required
     * @readonly
     */
    private ArtifactFactory artifactFactory;

    /**
     * The artifact metadata source to use.
     *
     * @component
     * @required
     * @readonly
     */
    private ArtifactMetadataSource artifactMetadataSource;

    /**
     * The artifact collector to use.
     *
     * @component
     * @required
     * @readonly
     */
    private ArtifactCollector artifactCollector;

    /**
     * The dependency tree builder to use.
     *
     * @component
     * @required
     * @readonly
     */
    private DependencyTreeBuilder dependencyTreeBuilder;

    /**
     * The toolchain manager to use.
     *
     * @component
     * @required
     * @readonly
     */
    protected ToolchainManager toolchainManager;

    /** @parameter default-value="${plugin.artifacts}" */
    private List<Artifact> pluginArtifacts;

    private VersionNumber _scalaVersionN;

    /**
     * This method resolves the dependency artifacts from the project.
     *
     * @param theProject The POM.
     * @return resolved set of dependency artifacts.
     *
     * @throws ArtifactResolutionException
     * @throws ArtifactNotFoundException
     * @throws InvalidDependencyVersionException
     */
    protected Set<Artifact> resolveDependencyArtifacts(MavenProject theProject) throws Exception {
        AndArtifactFilter filter = new AndArtifactFilter();
        filter.add(new ScopeArtifactFilter(Artifact.SCOPE_TEST));
        filter.add(new ArtifactFilter(){
            public boolean include(Artifact artifact) {
                return !artifact.isOptional();
            }
        });
        //TODO follow the dependenciesManagement and override rules
        Set<Artifact> artifacts = theProject.createArtifacts(artifactFactory, Artifact.SCOPE_RUNTIME, filter);
        for (Artifact artifact : artifacts) {
            resolver.resolve(artifact, remoteRepos, localRepo);
        }
        return artifacts;
    }

    /**
     * This method resolves all transitive dependencies of an artifact.
     *
     * @param artifact the artifact used to retrieve dependencies
     *
     * @return resolved set of dependencies
     *
     * @throws ArtifactResolutionException
     * @throws ArtifactNotFoundException
     * @throws ProjectBuildingException
     * @throws InvalidDependencyVersionException
     */
    protected Set<Artifact> resolveArtifactDependencies(Artifact artifact) throws Exception {
        Artifact pomArtifact = factory.createArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), "", "pom");
        MavenProject pomProject = mavenProjectBuilder.buildFromRepository(pomArtifact, remoteRepos, localRepo);
        return resolveDependencyArtifacts(pomProject);
    }

    public void addToClasspath(String groupId, String artifactId, String version, Set<String> classpath) throws Exception {
        addToClasspath(groupId, artifactId, version, classpath, true);
    }


    public void addToClasspath(String groupId, String artifactId, String version, Set<String> classpath, boolean addDependencies) throws Exception {
        addToClasspath(factory.createArtifact(groupId, artifactId, version, Artifact.SCOPE_RUNTIME, "jar"), classpath, addDependencies);
    }

    /**
     * added for classifier support.
     * @author Christoph Radig
     * @todo might want to merge with existing "addToClasspath" methods.
     */
    public void addToClasspath(String groupId, String artifactId, String version, String classifier, Set<String> classpath, boolean addDependencies) throws Exception {
      Dependency d = new Dependency();
      d.setGroupId(groupId);
      d.setArtifactId(artifactId);
      d.setVersion(version);
      d.setType("jar");
      d.setClassifier(classifier);
      d.setScope(Artifact.SCOPE_RUNTIME);
      addToClasspath(factory.createDependencyArtifact(d), classpath, addDependencies);
    }

    protected void addToClasspath(Artifact artifact, Set<String> classpath, boolean addDependencies) throws Exception {
        resolver.resolve(artifact, remoteRepos, localRepo);
        classpath.add(FileUtils.pathOf(artifact.getFile(), useCanonicalPath));
        if (addDependencies) {
            for (Artifact dep : resolveArtifactDependencies(artifact)) {
                addToClasspath(dep, classpath, addDependencies);
            }
        }
    }

    protected void addCompilerToClasspath(Set<String> classpath) throws Exception {
      classpath.add(FileUtils.pathOf(getCompilerJar(), useCanonicalPath));
      for (File dep : getCompilerDependencies()) {
        classpath.add(FileUtils.pathOf(dep, useCanonicalPath));
      }
    }

    protected void addLibraryToClasspath(Set<String> classpath) throws Exception {
      classpath.add(FileUtils.pathOf(getLibraryJar(), useCanonicalPath));
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
        } catch (MojoFailureException exc) {
            throw exc;
        } catch (RuntimeException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new MojoExecutionException("wrap: " + exc, exc);
        }
    }

    protected List<Dependency> getDependencies() {
        return project.getCompileDependencies();
    }

    protected VersionNumber findScalaVersion() throws Exception {
        if (_scalaVersionN == null) {
            String detectedScalaVersion = scalaVersion;
            if (StringUtils.isEmpty(detectedScalaVersion)) {
                detectedScalaVersion = findScalaVersionFromDependencies();
            }
            if (StringUtils.isEmpty(detectedScalaVersion)) {
                if (!"pom".equals( project.getPackaging().toLowerCase() )) {
                    getLog().warn("you don't define "+ getScalaOrganization() + ":" + SCALA_LIBRARY_ARTIFACTID + " as a dependency of the project");
                }
                detectedScalaVersion = "0.0.0";
            } else {
                // grappy hack to retrieve the SNAPSHOT version without timestamp,...
                // because if version is -SNAPSHOT and artifact is deploy with uniqueValue then the version
                // get from dependency is with the timestamp and a build number (the resolved version)
                // but scala-compiler with the same version could have different resolved version (timestamp,...)
                boolean isSnapshot = ArtifactUtils.isSnapshot(detectedScalaVersion);
                if (isSnapshot && !detectedScalaVersion.endsWith("-SNAPSHOT")) {
                    detectedScalaVersion = detectedScalaVersion.substring(0, detectedScalaVersion.lastIndexOf('-', detectedScalaVersion.lastIndexOf('-')-1)) + "-SNAPSHOT";
                }
            }
            if (StringUtils.isEmpty(detectedScalaVersion)) {
                throw new MojoFailureException("no scalaVersion detected or set");
            }
            if (StringUtils.isNotEmpty(scalaVersion)) {
                if (!scalaVersion.equals(detectedScalaVersion)) {
                    getLog().warn("scala library version define in dependencies doesn't match the scalaVersion of the plugin");
                }
                //getLog().info("suggestion: remove the scalaVersion from pom.xml"); //scalaVersion could be define in a parent pom where lib is not required
            }
            _scalaVersionN = new VersionNumber(detectedScalaVersion);
        }
        return _scalaVersionN;
    }

    private String findScalaVersionFromDependencies() throws Exception {
        return findVersionFromDependencies(getScalaOrganization(), SCALA_LIBRARY_ARTIFACTID);
    }

    //TODO refactor to do only one scan of dependencies to find version
    protected String findVersionFromDependencies(String groupId, String artifactId) throws Exception {
        String version = null;
        for (Dependency dep : getDependencies()) {
            if (groupId.equals(dep.getGroupId()) && artifactId.equals(dep.getArtifactId())) {
                version = dep.getVersion();
            }
        }
        if (StringUtils.isEmpty(version)) {
            List<Dependency> deps = new ArrayList<Dependency>();
            deps.addAll(project.getModel().getDependencies());
            if (project.getModel().getDependencyManagement() != null) {
                deps.addAll(project.getModel().getDependencyManagement().getDependencies());
            }
            for (Dependency dep : deps) {
                if (groupId.equals(dep.getGroupId()) && artifactId.equals(dep.getArtifactId())) {
                    version = dep.getVersion();
                }
            }
        }
        return version;
    }

    protected void checkScalaVersion() throws Exception {
        String sv = findScalaVersion().toString();
        if (StringUtils.isNotEmpty(scalaHome)) {
            getLog().warn(String.format("local scala-library.jar and scala-compiler.jar from scalaHome(%s) used instead of scala %s", scalaHome, sv));
        }
        if (checkMultipleScalaVersions) {
            checkCorrectVersionsOfScalaLibrary(sv);
        }
    }

    /** this method checks to see if there are multiple versions of the scala library
     * @throws Exception */
    private void checkCorrectVersionsOfScalaLibrary(String scalaDefVersion) throws Exception {
        getLog().debug("Checking for multiple versions of scala");
        //TODO - Make sure we handle bad artifacts....
        // TODO: note that filter does not get applied due to MNG-3236
        VersionNumber sv = new VersionNumber(scalaDefVersion);
        VersionNumber requiredScalaVersion = StringUtils.isNotEmpty(scalaCompatVersion) ? new VersionNumberMask(scalaCompatVersion) : sv;
        if (requiredScalaVersion.compareTo(sv) != 0) {
          String msg = String.format("Scala library detected %s doesn't match scala.compat.version : %s", sv, requiredScalaVersion);
          if(failOnMultipleScalaVersions) {
            getLog().error(msg);
            throw new MojoFailureException(msg);
          }
          getLog().warn(msg);
        }
        checkArtifactForScalaVersion(requiredScalaVersion, dependencyTreeBuilder.buildDependencyTree( project, localRepository, artifactFactory,
                    artifactMetadataSource, null, artifactCollector ));
    }


    /** Visits a node (and all dependencies) to see if it contains duplicate scala versions */
    private void checkArtifactForScalaVersion(VersionNumber requiredScalaVersion, DependencyNode rootNode) throws Exception {
        final CheckScalaVersionVisitor visitor = new CheckScalaVersionVisitor(requiredScalaVersion, getLog(), getScalaOrganization());

        CollectingDependencyNodeVisitor collectingVisitor = new CollectingDependencyNodeVisitor();
        DependencyNodeVisitor firstPassVisitor = new FilteringDependencyNodeVisitor( collectingVisitor, createScalaDistroDependencyFilter() );
        rootNode.accept( firstPassVisitor );

        DependencyNodeFilter secondPassFilter = new AncestorOrSelfDependencyNodeFilter( collectingVisitor.getNodes() );
        DependencyNodeVisitor filteredVisitor = new FilteringDependencyNodeVisitor( visitor, secondPassFilter );

        rootNode.accept( filteredVisitor );

        if(visitor.isFailed()) {
            visitor.logScalaDependents();
            if(failOnMultipleScalaVersions) {
                getLog().error("Multiple versions of scala libraries detected!");
                throw new MojoFailureException("Multiple versions of scala libraries detected!");
            }
            getLog().warn("Multiple versions of scala libraries detected!");
        }
    }

    /**
     * @return
     *          A filter to only extract artifacts deployed from scala distributions
     */
    private DependencyNodeFilter createScalaDistroDependencyFilter() {
        List<ArtifactFilter> filters = new ArrayList<ArtifactFilter>();
        filters.add(new ScalaDistroArtifactFilter(getScalaOrganization()));
        return new AndDependencyNodeFilter(filters);
    }



    protected abstract void doExecute() throws Exception;


    protected JavaMainCaller getScalaCommand() throws Exception {
        JavaMainCaller cmd = getEmptyScalaCommand(scalaClassName);
        cmd.addArgs(args);
        if (StringUtils.isNotEmpty(addScalacArgs)) {
          cmd.addArgs(StringUtils.split(addScalacArgs, "|"));
        }
        addCompilerPluginOptions(cmd);
        cmd.addJvmArgs(jvmArgs);
        return cmd;
    }

    protected JavaMainCaller getEmptyScalaCommand(String mainClass) throws Exception {

      //TODO - Fork or not depending on configuration?
      JavaMainCaller cmd;
      String toolcp = getToolClasspath();
      if(fork) {
        // HACK (better may need refactor)
        boolean bootcp = true;
        if (args != null) {
          for(String arg : args) {
            bootcp = bootcp && !"-nobootcp".equals(arg);
          }
        }
        String cp = bootcp ? "" : toolcp;
        bootcp = bootcp && !(StringUtils.isNotEmpty(addScalacArgs) && addScalacArgs.contains("-nobootcp"));
        // scalac with args in files
        // * works only since 2.8.0
        // * is buggy (don't manage space in path on windows)
        getLog().debug("use java command with args in file forced : " + forceUseArgFile);
        cmd = new JavaMainCallerByFork(this, mainClass, cp, null, null, forceUseArgFile, toolchainManager.getToolchainFromBuildContext("jdk", session));
        if (bootcp) {
          cmd.addJvmArgs("-Xbootclasspath/a:" + toolcp);
        }
      } else  {
        cmd = new JavaMainCallerInProcess(this, mainClass, toolcp, null, null);
      }
      return cmd;
    }

    private String getToolClasspath() throws Exception {
        Set<String> classpath = new LinkedHashSet<String>();
        addLibraryToClasspath(classpath);
        addCompilerToClasspath(classpath);
//        addToClasspath(SCALA_GROUPID, "scala-decoder", scalaVersion, classpath);
//        addToClasspath(SCALA_GROUPID, "scala-dbc", scalaVersion, classpath);
        if (dependencies != null) {
            for(BasicArtifact artifact: dependencies) {
                addToClasspath(artifact.groupId, artifact.artifactId, artifact.version, classpath);
            }
        }
        return MainHelper.toMultiPath(classpath.toArray(new String[classpath.size()]));
    }

    protected List<String> getScalaOptions() throws Exception {
        List<String> options = new ArrayList<String>();
        if (args != null) Collections.addAll(options, args);
        if (StringUtils.isNotEmpty(addScalacArgs)) {
            Collections.addAll(options, StringUtils.split(addScalacArgs, "|"));
        }
        options.addAll(getCompilerPluginOptions());
        return options;
    }

    protected List<String> getJavacOptions() throws Exception {
        List<String> options = new ArrayList<String>();
        if (javacArgs != null) Collections.addAll(options, javacArgs);
        if (StringUtils.isNotEmpty(addJavacArgs)) {
            Collections.addAll(options, StringUtils.split(addJavacArgs, "|"));
        }

        // issue #116
        if (javacGenerateDebugSymbols) {
            options.add("-g");
        }
        if (target != null) {
            options.add("-target");
            options.add(target);
        }
        if (source != null) {
          options.add("-source");
          options.add(source);
        }
        if (encoding != null) {
          options.add("-encoding");
          options.add(encoding);
        }
        return options;
    }

    protected File getLibraryJar() throws Exception {
      if (StringUtils.isNotEmpty(scalaHome)) {
        File lib = new File(scalaHome, "lib");
        return new File(lib, "scala-library.jar");
      }
      return getArtifactJar(getScalaOrganization(), SCALA_LIBRARY_ARTIFACTID, findScalaVersion().toString());
    }

    protected File getCompilerJar() throws Exception {
      if(StringUtils.isNotEmpty(scalaHome)) {
        File lib = new File(scalaHome, "lib");
        return new File(lib, "scala-compiler.jar");
      }
      return getArtifactJar(getScalaOrganization(), SCALA_COMPILER_ARTIFACTID, findScalaVersion().toString());
    }

    protected List<File> getCompilerDependencies() throws Exception {
      List<File> d = new ArrayList<File>();
      if(StringUtils.isEmpty(scalaHome)) {
        for (Artifact artifact : getAllDependencies(getScalaOrganization(), SCALA_COMPILER_ARTIFACTID, findScalaVersion().toString())) {
          d.add(artifact.getFile());
        }
      } else {
        for(File f : new File(scalaHome, "lib").listFiles()) {
          String name = f.getName();
          if (name.endsWith(".jar") && !name.contains("scala-library") && !name.contains("scala-compiler")) {
            d.add(f);
          }
        }
      }
      return d;
    }

    protected File getArtifactJar(String groupId, String artifactId, String version) throws Exception {
        Artifact artifact = factory.createArtifact(groupId, artifactId, version, Artifact.SCOPE_RUNTIME, "jar");
        resolver.resolve(artifact, remoteRepos, localRepo);
        return artifact.getFile();
    }

    protected File getArtifactJar(String groupId, String artifactId, String version, String classifier) throws Exception {
        Artifact artifact = factory.createArtifactWithClassifier(groupId, artifactId, version, "jar", classifier);
        resolver.resolve(artifact, remoteRepos, localRepo);
        return artifact.getFile();
    }

    protected Set<Artifact> getAllDependencies(String groupId, String artifactId, String version) throws Exception {
        Set<Artifact> result = new HashSet<Artifact>();
        Artifact pom = factory.createArtifact(groupId, artifactId, version, "", "pom");
        MavenProject p = mavenProjectBuilder.buildFromRepository(pom, remoteRepos, localRepo);
        Set<Artifact> d = resolveDependencyArtifacts(p);
        result.addAll(d);
        for (Artifact dependency : d) {
            Set<Artifact> transitive = getAllDependencies(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
            result.addAll(transitive);
        }
        return result;
    }

    /**
     * @return
     *           This returns whether or not the scala version can support having java sent into the compiler
     */
    protected boolean isJavaSupportedByCompiler() throws Exception {
        return findScalaVersion().compareTo(new VersionNumber("2.7.2")) >= 0;
    }


    /**
     * Adds appropriate compiler plugins to the scalac command.
     * @param scalac
     * @throws Exception
     */
    protected void addCompilerPluginOptions(JavaMainCaller scalac) throws Exception {
        for (String option : getCompilerPluginOptions()) {
            scalac.addArgs(option);
        }
    }

    protected List<String> getCompilerPluginOptions() throws Exception {
        List<String> options = new ArrayList<String>();
        for (String plugin : getCompilerPlugins()) {
            options.add("-Xplugin:" + plugin);
        }
        return options;
    }

    /**
     * Retrieves a list of paths to scala compiler plugins.
     *
     * @return The list of plugins
     * @throws Exception
     */
    private Set<String> getCompilerPlugins() throws Exception {
        Set<String> plugins = new HashSet<String>();
        if (compilerPlugins != null) {
            Set<String> ignoreClasspath = new LinkedHashSet<String>();
            addCompilerToClasspath(ignoreClasspath);
            addLibraryToClasspath(ignoreClasspath);
            for (BasicArtifact artifact : compilerPlugins) {
                getLog().info("compiler plugin: " + artifact.toString());
                // TODO - Ensure proper scala version for plugins
                Set<String> pluginClassPath = new HashSet<String>();
                //TODO - Pull in transitive dependencies.
                addToClasspath(artifact.groupId, artifact.artifactId, artifact.version, artifact.classifier, pluginClassPath, false);
                pluginClassPath.removeAll(ignoreClasspath);
                plugins.addAll(pluginClassPath);
            }
        }
        return plugins;
    }

    protected String findVersionFromPluginArtifacts(String groupId, String artifactId) throws Exception {
        String version = null;
        for (Artifact art : pluginArtifacts) {
            if (groupId.equals(art.getGroupId()) && artifactId.equals(art.getArtifactId())) {
                version = art.getVersion();
            }
        }
        return version;
    }

    protected File getPluginArtifactJar(String groupId, String artifactId, String version) throws Exception {
        return getPluginArtifactJar(groupId, artifactId, version, null);
    }

    protected File getPluginArtifactJar(String groupId, String artifactId, String version, String classifier) throws Exception {
        Artifact artifact = null;
        for (Artifact art : pluginArtifacts) {
            if (groupId.equals(art.getGroupId()) && artifactId.equals(art.getArtifactId()) && version.equals(art.getVersion())){
            	if ((classifier == null && art.getClassifier() == null) || (classifier != null && classifier.equals(art.getClassifier()))) {
            		artifact = art;
            	}
            }
        }
        if (artifact == null) {
	    	String msg = String.format("can't find artifact %s::%s::%s-%s", groupId, artifactId, version, classifier);
	    	getLog().error(msg);
	    	throw new Exception(msg);
        }
        return artifact.getFile();
    }
}

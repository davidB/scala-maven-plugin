package scala_maven;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
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
import org.apache.maven.toolchain.ToolchainManager;
import org.codehaus.plexus.util.StringUtils;

import scala_maven_dependency.CheckScalaVersionVisitor;
import scala_maven_dependency.ScalaDistroArtifactFilter;
import scala_maven_executions.JavaMainCaller;
import scala_maven_executions.JavaMainCallerByFork;
import scala_maven_executions.JavaMainCallerInProcess;
import scala_maven_executions.MainHelper;

public abstract class ScalaMojoSupport extends AbstractMojo {

    public static final String SCALA_LIBRARY_ARTIFACTID = "scala-library";
    public static final String SCALA_COMPILER_ARTIFACTID = "scala-compiler";

    /**
     * Constant {@link String} for "pom". Used to specify the Maven POM artifact
     * type.
     */
    protected static final String POM = "pom";

    /**
     * Constant {@link String} for "jar". Used to specify the Maven JAR artifact
     * type.
     */
    protected static final String JAR = "jar";

    /**
     * The maven project.
     *
     */
    @Parameter(property = "project", required = true, readonly = true)
    protected MavenProject project;

    /**
     * The Maven Session Object
     *
     */
    @Parameter(property = "session", required = true, readonly = true)
    protected MavenSession session;

    /**
     * Contains the full list of projects in the reactor.
     *
     */
    @Parameter(defaultValue = "${reactorProjects}", readonly = true, required = true)
    protected List<MavenProject> reactorProjects;

    /**
     * Used to look up Artifacts in the remote repository.
     *
     */
    @Component
    protected RepositorySystem factory;

    /**
     * Used to look up Artifacts in the remote repository.
     *
     */
    @Component
    protected ArtifactResolver resolver;

    /**
     * Location of the local repository.
     *
     */
    @Parameter(property = "localRepository", readonly = true, required = true)
    protected ArtifactRepository localRepo;

    /**
     * List of Remote Repositories used by the resolver
     *
     */
    @Parameter(property = "project.remoteArtifactRepositories", readonly = true, required = true)
    protected List<ArtifactRepository> remoteRepos;

    /**
     * Additional dependencies/jar to add to classpath to run "scalaClassName"
     * (scope and optional field not supported)
     * ex :
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
     *
     */
    @Parameter
    protected BasicArtifact[] dependencies;

    /**
     * Compiler plugin dependencies to use when compiling.
     * ex:
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
     *
     */
    @Parameter
    protected BasicArtifact[] compilerPlugins;

    /**
     * Jvm Arguments.
     *
     */
    @Parameter
    protected String[] jvmArgs;

    /**
     * compiler additional arguments
     *
     */
    @Parameter
    protected String[] args;

    /**
     * Additional parameter to use to call the main class.
     * Use this parameter only from command line
     * ("-DaddScalacArgs=arg1|arg2|arg3|..."), not from pom.xml.
     * To define compiler arguments in pom.xml see the "args" parameter.
     *
     */
    @Parameter(property = "addScalacArgs")
    protected String addScalacArgs;

    /**
     * className (FQN) of the scala tool to provide as
     *
     */
    @Parameter(required = true, property = "maven.scala.className", defaultValue = "scala.tools.nsc.Main")
    protected String scalaClassName;

    /**
     * Scala 's version to use.
     * (property 'maven.scala.version' replaced by 'scala.version')
     *
     */
    @Parameter(property = "scala.version")
    private String scalaVersion;

    /**
     * Organization/group ID of the Scala used in the project.
     * Default value is 'org.scala-lang'.
     * This is an advanced setting used for clones of the Scala Language.
     * It should be disregarded in standard use cases.
     *
     */
    @Parameter(property = "scala.organization", defaultValue = "org.scala-lang")
    private String scalaOrganization;

    public String getScalaOrganization() {
        return scalaOrganization;
    }

    /**
     * Scala 's version to use to check binary compatibility (like suffix in
     * artifactId of dependency).
     * If it is defined then it is used to checkMultipleScalaVersions
     *
     */
    @Parameter(property = "scala.compat.version")
    private String scalaCompatVersion;

    /**
     * Path to Scala installation to use instead of the artifact (define as
     * dependencies).
     *
     */
    @Parameter(property = "scala.home")
    private String scalaHome;

    /**
     * Arguments for javac (when using incremental compiler).
     *
     */
    @Parameter(property = "javacArgs")
    protected String[] javacArgs;

    /**
     * Whether to instruct javac to generate debug symbols (when using incremental
     * compiler)
     *
     * @see <a href=
     *      "http://maven.apache.org/plugins/maven-compiler-plugin/compile-mojo.html#debug">://maven.apache.org/plugins/maven-compiler-plugin/compile-mojo.html#debug</a>
     *
     */
    @Parameter(property = "javacGenerateDebugSymbols", defaultValue = "true")
    protected boolean javacGenerateDebugSymbols = true;

    /**
     * Alternative method for specifying javac arguments (when using incremental
     * compiler).
     * Can be used from command line with -DaddJavacArgs=arg1|arg2|arg3|... rather
     * than in pom.xml.
     *
     */
    @Parameter(property = "addJavacArgs")
    protected String addJavacArgs;

    /**
     * The -source argument for the Java compiler (when using incremental compiler).
     *
     */
    @Parameter(property = "maven.compiler.source")
    protected String source;

    /**
     * The -target argument for the Java compiler (when using incremental compiler).
     *
     */
    @Parameter(property = "maven.compiler.target")
    protected String target;

    /**
     * The -encoding argument for the Java compiler. (when using incremental
     * compiler).
     *
     */
    @Parameter(property = "project.build.sourceEncoding", defaultValue = "UTF-8")
    protected String encoding;

    /**
     * Display the command line called ?
     * (property 'maven.scala.displayCmd' replaced by 'displayCmd')
     *
     */
    @Parameter(property = "displayCmd", defaultValue = "false", required = true)
    public boolean displayCmd;

    /**
     * Forks the execution of scalac into a separate process.
     *
     */
    @Parameter(defaultValue = "true")
    protected boolean fork = true;

    /**
     * Force the use of an external ArgFile to run any forked process.
     *
     */
    @Parameter(defaultValue = "false")
    protected boolean forceUseArgFile = false;

    /**
     * Check if every dependencies use the same version of scala-library or
     * scala.compat.version.
     *
     */
    @Parameter(property = "maven.scala.checkConsistency", defaultValue = "true")
    protected boolean checkMultipleScalaVersions;

    /**
     * Determines if a detection of multiple scala versions in the dependencies will
     * cause the build to fail.
     *
     */
    @Parameter(defaultValue = "false")
    protected boolean failOnMultipleScalaVersions = false;

    /**
     * Should use CanonicalPath to normalize path (true =&gt; getCanonicalPath, false
     * =&gt; getAbsolutePath)
     *
     * @see <a href=
     *      "https://github.com/davidB/maven-scala-plugin/issues/50">https://github.com/davidB/maven-scala-plugin/issues/50</a>
     */
    @Parameter(property = "maven.scala.useCanonicalPath", defaultValue = "true")
    protected boolean useCanonicalPath = true;

    /**
     * Artifact factory, needed to download source jars.
     *
     */
    @Component
    protected MavenProjectBuilder mavenProjectBuilder;

    /**
     * The artifact repository to use.
     *
     */
    @Parameter(property = "localRepository", required = true, readonly = true)
    private ArtifactRepository localRepository;

    /**
     * The artifact factory to use.
     *
     */
    @Component
    private ArtifactFactory artifactFactory;

    /**
     * The artifact metadata source to use.
     */
    @Component
    private ArtifactMetadataSource artifactMetadataSource;

    /**
     * The artifact collector to use.
     *
     */
    @Component
    private ArtifactCollector artifactCollector;

    /**
     * The dependency tree builder to use.
     *
     */
    @Component
    private DependencyGraphBuilder dependencyTreeBuilder;

    /**
     * The toolchain manager to use.
     */
    @Component
    protected ToolchainManager toolchainManager;

    /**
     * List of artifacts to run plugin
     *
     */
    @Parameter(defaultValue="${plugin.artifacts}")
    private List<Artifact> pluginArtifacts;

    private VersionNumber _scalaVersionN;

    /**
     * Constructs an {@link Artifact} for Scala Compiler.
     *
     * @param scalaVersion the version of the Scala Compiler/Library we are
     *        using for this execution.
     *
     * @return a {@link Artifact} for the Scala Compiler.
     */
    protected final Artifact scalaCompilerArtifact(final String scalaVersion) {
        return this.factory.createArtifact(this.getScalaOrganization(),
                                           ScalaMojoSupport.SCALA_COMPILER_ARTIFACTID,
                                           scalaVersion,
                                           "",
                                           ScalaMojoSupport.POM);
    }

    /**
     * This method resolves all transitive dependencies of an artifact.
     *
     * @param artifact the {@link Artifact} used to retrieve dependencies.
     *
     * @return resolved {@link Set} of dependencies.
     *
     * @throws {@link Exception} when various artifact resolution mechanisms fail.
     */
    protected final Set<Artifact> resolveArtifactDependencies(final Artifact artifact) throws Exception {
        final AndArtifactFilter filter = new AndArtifactFilter();
        filter.add(new ScopeArtifactFilter(Artifact.SCOPE_TEST));
        filter.add(new ArtifactFilter(){
                public boolean include(Artifact artifact) {
                    return !artifact.isOptional();
                }
            });

        // Use the collection filter as the resolution filter.
        return resolveDependencyArtifacts(artifact,
                                          filter,
                                          filter);
    }

    /**
     * This method resolves all transitive dependencies of an artifact.
     *
     * @param artifact the {@link Artifact} used to retrieve dependencies.
     * @param collectionFilter an {@link ArtifactFilter} used to determine which
     *        members of the dependency graph should be included in resolution.
     * @param collectionFilter an {@link ArtifactFilter} used to determine which
     *        members dependency graph should be downloaded.
     *
     * @return resolved {@link Set} of dependencies.
     *
     * @throws {@link Exception} when various artifact resolution mechanisms fail.
     */
    protected final Set<Artifact> resolveDependencyArtifacts(final Artifact artifact,
                                                             final ArtifactFilter collectionFilter,
                                                             final ArtifactFilter resolutionFilter) throws Exception {
        return this.resolveDependencyArtifacts(artifact,
                                               collectionFilter,
                                               resolutionFilter,
                                               this.remoteRepos,
                                               this.localRepo);
    }

    /**
     * This method resolves all transitive dependencies of an artifact.
     *
     * @param artifact the {@link Artifact} used to retrieve dependencies.
     * @param collectionFilter an {@link ArtifactFilter} used to determine which
     *        members of the dependency graph should be included in resolution.
     * @param collectionFilter an {@link ArtifactFilter} used to determine which
     *        members dependency graph should be downloaded.
     * @param remoteRepositories a {@link List} of remote {@link
     *        ArtifactRespository} values to used for dependency resolution of
     *        the provided {@link Artifact}.
     * @param localRepository the local {@link ArtifactRepository} to use for
     *        dependency resolution of the given {@link Artifact}.
     *
     * @return resolved {@link Set} of dependencies.
     *
     * @throws {@link Exception} when various artifact resolution mechanisms fail.
     */
    protected final Set<Artifact> resolveDependencyArtifacts(final Artifact artifact,
                                                             final ArtifactFilter collectionFilter,
                                                             final ArtifactFilter resolutionFilter,
                                                             final List<ArtifactRepository> remoteRepositories,
                                                             final ArtifactRepository localRepository) throws Exception {
        final ArtifactResolutionRequest arr =
            this.createArtifactResolutionRequest(artifact,
                                                 collectionFilter,
                                                 resolutionFilter,
                                                 remoteRepositories,
                                                 localRepository);

        //TODO follow the dependenciesManagement and override rules
        return this.resolver.resolve(arr).getArtifacts();
    }

    /**
     * Create a {@link ArtifactResolutionRequest}.
     *
     * @param artifact the {@link Artifact} used to retrieve dependencies.
     * @param collectionFilter an {@link ArtifactFilter} used to determine which
     *        members of the dependency graph should be included in resolution.
     * @param collectionFilter an {@link ArtifactFilter} used to determine which
     *        members dependency graph should be downloaded.
     * @param remoteRepositories a {@link List} of remote {@link
     *        ArtifactRespository} values to used for dependency resolution of
     *        the provided {@link Artifact}.
     * @param localRepository the local {@link ArtifactRepository} to use for
     *        dependency resolution of the given {@link Artifact}.
     *
     * @return an {@link ArtifactResolutionRequest}, typically used for
     *         dependency resolution requests against an {@link
     *         ArtifactResolver}.
     */
    private ArtifactResolutionRequest createArtifactResolutionRequest(final Artifact artifact,
                                                                      final ArtifactFilter collectionFilter,
                                                                      final ArtifactFilter resolutionFilter,
                                                                      final List<ArtifactRepository> remoteRepositories,
                                                                      final ArtifactRepository localRepository) {
        final ArtifactResolutionRequest arr = new ArtifactResolutionRequest();

        arr.setArtifact(artifact);
        arr.setCollectionFilter(collectionFilter);
        arr.setResolutionFilter(resolutionFilter);
        arr.setResolveRoot(false);
        arr.setResolveTransitively(true);
        arr.setRemoteRepositories(remoteRepositories);
        arr.setLocalRepository(localRepository);

        return arr;
    }

    public void addToClasspath(String groupId, String artifactId, String version, Set<String> classpath) throws Exception {
        addToClasspath(groupId, artifactId, version, classpath, true);
    }


    public void addToClasspath(String groupId, String artifactId, String version, Set<String> classpath, boolean addDependencies) throws Exception {
        addToClasspath(factory.createArtifact(groupId, artifactId, version, Artifact.SCOPE_RUNTIME, ScalaMojoSupport.JAR), classpath, addDependencies);
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
      d.setType(ScalaMojoSupport.JAR);
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
                if (!ScalaMojoSupport.POM.equals( project.getPackaging().toLowerCase() )) {
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
        ProjectBuildingRequest request = project.getProjectBuildingRequest();
        request.setProject(project);
        checkArtifactForScalaVersion(requiredScalaVersion, dependencyTreeBuilder.buildDependencyGraph(request, null));
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
        List<DependencyNodeFilter> filters = new ArrayList<DependencyNodeFilter>();
        filters.add(new ScalaDistroArtifactFilter(getScalaOrganization()));
        return new AndDependencyNodeFilter(filters);
    }



    protected abstract void doExecute() throws Exception;


    protected JavaMainCaller getScalaCommand() throws Exception {
        return this.getScalaCommand(this.fork,
                                    this.scalaClassName);
    }

    /**
     * Get a {@link JavaMainCaller} used invoke a Java process. Typically this
     * will be one of the Scala utilities (Compiler, ScalaDoc, REPL, etc.).
     * <p>
     * This method does some setup on the {@link JavaMainCaller} which is not
     * done by merely invoking {@code new} on one of the
     * implementations. Specifically, it adds any Scala compiler plugin options,
     * JVM options, and Scalac options defined on the plugin.
     *
     * @param forkOverride override the setting for {@link #fork}. Currently
     *        this should only be set if you are invoking the REPL.
     * @param mainClass the JVM main class to invoke.
     *
     * @return a {@link JavaMainCaller} to use to invoke the given command.
     */
    protected final JavaMainCaller getScalaCommand(final boolean forkOverride,
                                                   final String mainClass) throws Exception {
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
     * Get a {@link JavaMainCaller} used invoke a Java process. Typically this
     * will be one of the Scala utilities (Compiler, ScalaDoc, REPL, etc.).
     *
     * @param mainClass the JVM main class to invoke.
     *
     * @return a {@link JavaMainCaller} to use to invoke the given command.
     */
    protected final JavaMainCaller getEmptyScalaCommand(final String mainClass) throws Exception {
        return this.getEmptyScalaCommand(mainClass, this.fork);
    }

    /**
     * Get a {@link JavaMainCaller} used invoke a Java process. Typically this
     * will be one of the Scala utilities (Compiler, ScalaDoc, REPL, etc.).
     *
     * @param mainClass the JVM main class to invoke.
     * @param forkOverride override the setting for {@link #fork}. Currently
     *        this should only be set if you are invoking the REPL.
     *
     * @return a {@link JavaMainCaller} to use to invoke the given command.
     */
    protected JavaMainCaller getEmptyScalaCommand(final String mainClass,
                                                  final boolean forkOverride) throws Exception {

        // If we are deviating from the plugin settings, let the user know
        // what's going on.
        if (forkOverride != this.fork) {
            super.getLog().info("Fork behavior overridden");
            super.getLog().info(String.format("Fork for this execution is %s.", String.valueOf(forkOverride)));
        }

        //TODO - Fork or not depending on configuration?
        JavaMainCaller cmd;
        String toolcp = getToolClasspath();
        if(forkOverride) {
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
        Artifact artifact = factory.createArtifact(groupId, artifactId, version, Artifact.SCOPE_RUNTIME, ScalaMojoSupport.JAR);
        resolver.resolve(artifact, remoteRepos, localRepo);
        return artifact.getFile();
    }

    protected File getArtifactJar(String groupId, String artifactId, String version, String classifier) throws Exception {
        Artifact artifact = factory.createArtifactWithClassifier(groupId, artifactId, version, ScalaMojoSupport.JAR, classifier);
        resolver.resolve(artifact, remoteRepos, localRepo);
        return artifact.getFile();
    }

    protected Set<Artifact> getAllDependencies(String groupId, String artifactId, String version) throws Exception {
        Set<Artifact> result = new HashSet<Artifact>();
        Artifact pom = factory.createArtifact(groupId, artifactId, version, "", ScalaMojoSupport.POM);
        Set<Artifact> d = resolveArtifactDependencies(pom);
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

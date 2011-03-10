/*
 * Copyright 2007 scala-tools.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org_scala_tools_maven;

import java.io.File;
import java.util.ArrayList;
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
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.filter.AncestorOrSelfDependencyNodeFilter;
import org.apache.maven.shared.dependency.tree.filter.AndDependencyNodeFilter;
import org.apache.maven.shared.dependency.tree.filter.DependencyNodeFilter;
import org.apache.maven.shared.dependency.tree.traversal.CollectingDependencyNodeVisitor;
import org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor;
import org.apache.maven.shared.dependency.tree.traversal.FilteringDependencyNodeVisitor;
import org.codehaus.plexus.util.StringUtils;
import org_scala_tools_maven_dependency.CheckScalaVersionVisitor;
import org_scala_tools_maven_dependency.ScalaDistroArtifactFilter;
import org_scala_tools_maven_executions.JavaMainCaller;
import org_scala_tools_maven_executions.JavaMainCallerByFork;
import org_scala_tools_maven_executions.JavaMainCallerInProcess;
import org_scala_tools_maven_executions.MainHelper;

public abstract class ScalaMojoSupport extends AbstractMojo {

    public static final String SCALA_GROUPID= "org.scala-lang";
    public static final String SCALA_COMPILER_ARTIFACTID= "scala-compiler";
    public static final String SCALA_LIBRARY_ARTIFACTID= "scala-library";
    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * Used to look up Artifacts in the remote repository.
     *
     * @component
     * @required
     * @readonly
     */
    protected ArtifactFactory factory;

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
     * @parameter expression="${localRepository}"
     * @readonly
     * @required
     */
    protected ArtifactRepository localRepo;

    /**
     * List of Remote Repositories used by the resolver
     *
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @readonly
     * @required
     */
    protected List<?> remoteRepos;

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
     * className (FQN) of the scala tool to provide as
     *
     * @required
     * @parameter expression="${maven.scala.className}"
     *            default-value="scala.tools.nsc.Main"
     */
    protected String scalaClassName;

    /**
     * Scala 's version to use.
     * (property 'maven.scala.version' replaced by 'scala.version')
     *
     * @parameter expression="${scala.version}"
     */
    private String scalaVersion;

    /**
     * Path to Scala installation.
     *
     * @parameter expression="${scala.home}"
     */
    private String scalaHome;

    /**
     * Display the command line called ?
     * (property 'maven.scala.displayCmd' replaced by 'displayCmd')
     *
     * @required
     * @parameter expression="${displayCmd}"
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
     * Check if every dependencies use the same version of scala-library.
     *
     * @parameter expression="${maven.scala.checkConsistency}" default-value="true"
     */
    protected boolean checkMultipleScalaVersions;

    /**
     * Determines if a detection of multiple scala versions in the dependencies will cause the build to fail.
     *
     * @parameter default-value="false"
     */
    protected boolean failOnMultipleScalaVersions = false;
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
     * @parameter expression="${localRepository}"
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
    @SuppressWarnings("unchecked")
    protected Set<Artifact> resolveDependencyArtifacts(MavenProject theProject) throws Exception {
        AndArtifactFilter filter = new AndArtifactFilter();
        filter.add(new ScopeArtifactFilter(Artifact.SCOPE_TEST));
        filter.add(new ArtifactFilter(){
            public boolean include(Artifact artifact) {
                return !artifact.isOptional();
            }
        });
        //TODO follow the dependenciesManagement and override rules
        Set<Artifact> artifacts = theProject.createArtifacts(factory, Artifact.SCOPE_RUNTIME, filter);
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

    protected void addCompilerToClasspath(String version, Set<String> classpath) throws Exception {
        if(StringUtils.isEmpty(scalaHome)) {
            addToClasspath(SCALA_GROUPID, SCALA_COMPILER_ARTIFACTID, version, classpath);
        } else {
            // Note that in this case we have to ignore dependencies.
            File lib = new File(scalaHome, "lib");
            File compilerJar = new File(lib, "scala-compiler.jar");
            classpath.add(compilerJar.toString());
        }
    }

    protected void addToClasspath(Artifact artifact, Set<String> classpath, boolean addDependencies) throws Exception {
        resolver.resolve(artifact, remoteRepos, localRepo);
        classpath.add(artifact.getFile().getCanonicalPath());
        if (addDependencies) {
            for (Artifact dep : resolveArtifactDependencies(artifact)) {
                //classpath.add(dep.getFile().getCanonicalPath());
                addToClasspath(dep, classpath, addDependencies);
            }
        }
    }

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

    @SuppressWarnings("unchecked")
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
                    getLog().warn("you don't define "+SCALA_GROUPID + ":" + SCALA_LIBRARY_ARTIFACTID + " as a dependency of the project");
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

    //TODO refactor to do only one scan of dependency to find all scala-version
    private String findScalaVersionFromDependencies() throws Exception {
        String detectedScalaVersion = null;
        for (Dependency dep : getDependencies()) {
            if (SCALA_GROUPID.equals(dep.getGroupId()) && SCALA_LIBRARY_ARTIFACTID.equals(dep.getArtifactId())) {
                detectedScalaVersion = dep.getVersion();
            }
        }
        if (StringUtils.isEmpty(detectedScalaVersion)) {
            List<Dependency> deps = new ArrayList<Dependency>();
            deps.addAll(project.getModel().getDependencies());
            if (project.getModel().getDependencyManagement() != null) {
                deps.addAll(project.getModel().getDependencyManagement().getDependencies());
            }
            for (Dependency dep : deps) {
                if (SCALA_GROUPID.equals(dep.getGroupId()) && SCALA_LIBRARY_ARTIFACTID.equals(dep.getArtifactId())) {
                    detectedScalaVersion = dep.getVersion();
                }
            }
        }
        return detectedScalaVersion;
    }

    protected void checkScalaVersion() throws Exception {
        if (checkMultipleScalaVersions) {
            checkCorrectVersionsOfScalaLibrary(findScalaVersion().toString());
        }
    }
    
    /** this method checks to see if there are multiple versions of the scala library
     * @throws Exception */
    private void checkCorrectVersionsOfScalaLibrary(String requiredScalaVersion) throws Exception {
        getLog().info("Checking for multiple versions of scala");
        //TODO - Make sure we handle bad artifacts....
        // TODO: note that filter does not get applied due to MNG-3236
            checkArtifactForScalaVersion(requiredScalaVersion, dependencyTreeBuilder.buildDependencyTree( project, localRepository, artifactFactory,
                    artifactMetadataSource, null, artifactCollector ));
    }


    /** Visits a node (and all dependencies) to see if it contains duplicate scala versions */
    private void checkArtifactForScalaVersion(String requiredScalaVersion, DependencyNode rootNode) throws Exception {
        final CheckScalaVersionVisitor visitor = new CheckScalaVersionVisitor(requiredScalaVersion, getLog());

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
        filters.add(new ScalaDistroArtifactFilter());
        return new AndDependencyNodeFilter(filters);
    }



    protected abstract void doExecute() throws Exception;


    protected JavaMainCaller getScalaCommand() throws Exception {
        JavaMainCaller cmd = getEmptyScalaCommand(scalaClassName);
        cmd.addArgs(args);
        addCompilerPluginOptions(cmd);
        cmd.addJvmArgs(jvmArgs);
        return cmd;
    }

    protected JavaMainCaller getEmptyScalaCommand(String mainClass) throws Exception {
        //TODO - Fork or not depending on configuration?
        JavaMainCaller cmd;
        if(fork) {
           // scalac with args in files
           // * works only since 2.8.0
           // * is buggy (don't manage space in path on windows)
            getLog().debug("use java command with args in file forced : " + forceUseArgFile);
            cmd = new JavaMainCallerByFork(this, mainClass, getToolClasspath(), null, null, forceUseArgFile);
        } else  {
            cmd = new JavaMainCallerInProcess(this, mainClass, getToolClasspath(), null, null);
        }
        cmd.addJvmArgs("-Xbootclasspath/a:"+ getBootClasspath());
        return cmd;
    }

    private String getToolClasspath() throws Exception {
        Set<String> classpath = new LinkedHashSet<String>();
        addCompilerToClasspath(findScalaVersion().toString(), classpath);
//        addToClasspath(SCALA_GROUPID, "scala-decoder", scalaVersion, classpath);
//        addToClasspath(SCALA_GROUPID, "scala-dbc", scalaVersion, classpath);
        if (dependencies != null) {
            for(BasicArtifact artifact: dependencies) {
                addToClasspath(artifact.groupId, artifact.artifactId, artifact.version, classpath);
            }
        }
        return MainHelper.toMultiPath(classpath.toArray(new String[classpath.size()]));
    }

    private String getBootClasspath() throws Exception {
        Set<String> classpath = new HashSet<String>();
        addToClasspath(SCALA_GROUPID, SCALA_LIBRARY_ARTIFACTID, findScalaVersion().toString(), classpath);
        return MainHelper.toMultiPath(classpath.toArray(new String[classpath.size()]));
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
        for (String plugin : getCompilerPlugins()) {
            scalac.addArgs("-Xplugin:" + plugin);
        }
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
            Set<String> ignoreClasspath = new HashSet<String>();
            String sv = findScalaVersion().toString();
            addCompilerToClasspath(sv, ignoreClasspath);
            addToClasspath(SCALA_GROUPID, SCALA_LIBRARY_ARTIFACTID, sv, ignoreClasspath);
            for (BasicArtifact artifact : compilerPlugins) {
                getLog().info("compiler plugin: " + artifact.toString());
                // TODO - Ensure proper scala version for plugins
                Set<String> pluginClassPath = new HashSet<String>();
                //TODO - Pull in transitive dependencies.
                addToClasspath(artifact.groupId, artifact.artifactId, artifact.version, pluginClassPath, false);
                pluginClassPath.removeAll(ignoreClasspath);
                plugins.addAll(pluginClassPath);
            }
        }
        return plugins;
    }


}

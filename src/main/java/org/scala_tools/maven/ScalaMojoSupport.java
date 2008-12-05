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
package org.scala_tools.maven;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
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
import org.codehaus.plexus.util.StringUtils;

abstract class ScalaMojoSupport extends AbstractMojo {

    public static final String SCALA_GROUPID= "org.scala-lang";
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
     * @parameter expression="${component.org.apache.maven.artifact.factory.ArtifactFactory}"
     * @required
     * @readonly
     */
    protected ArtifactFactory factory;

    /**
     * Used to look up Artifacts in the remote repository.
     *
     * @parameter expression="${component.org.apache.maven.artifact.resolver.ArtifactResolver}"
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
     *    &lt;dependencies>
     *      &lt;dependency>
     *        &lt;groupId>org.scala-tools&lt;/groupId>
     *        &lt;artifactId>scala-compiler-addon&lt;/artifactId>
     *        &lt;version>1.0-SNAPSHOT&lt;/version>
     *      &lt;/dependency>
     *    &lt;/dependencies>
     * @parameter
     */
    protected BasicArtifact[] dependencies;

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
     * Scala 's version to use
     * @parameter expression="${maven.scala.version}"
     */
    protected String scalaVersion;

    /**
     * Display the command line called ?
     *
     * @required
     * @parameter expression="${maven.scala.displayCmd}"
     *            default-value="false"
     */
    protected boolean displayCmd;

    /**
     * Artifact factory, needed to download source jars.
     *
     * @component role="org.apache.maven.project.MavenProjectBuilder"
     * @required
     * @readonly
     */
    protected MavenProjectBuilder mavenProjectBuilder;

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

    protected void addToClasspath(String groupId, String artifactId, String version, Set<String> classpath) throws Exception {
        addToClasspath(factory.createArtifact(groupId, artifactId, version, Artifact.SCOPE_RUNTIME, "jar"), classpath);
    }

    protected void addToClasspath(Artifact artifact, Set<String> classpath) throws Exception {
        resolver.resolve(artifact, remoteRepos, localRepo);
        classpath.add(artifact.getFile().getCanonicalPath());
        for(Artifact dep: resolveArtifactDependencies(artifact)) {
            classpath.add(dep.getFile().getCanonicalPath());
        }
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
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

    @SuppressWarnings("unchecked")
    protected void checkScalaVersion() throws Exception {
        String detectedScalaVersion = null;
        for (Iterator it = getDependencies().iterator(); it.hasNext();) {
            Dependency dep = (Dependency) it.next();
            if (SCALA_GROUPID.equals(dep.getGroupId()) && SCALA_LIBRARY_ARTIFACTID.equals(dep.getArtifactId())) {
                detectedScalaVersion = dep.getVersion();
            }
        }
        if (StringUtils.isEmpty(detectedScalaVersion)) {
            getLog().warn("you don't define "+SCALA_GROUPID + ":" + SCALA_LIBRARY_ARTIFACTID + " as a dependency of the project");
        } else {
            if (StringUtils.isNotEmpty(scalaVersion)) {
                if (!scalaVersion.equals(detectedScalaVersion)) {
                    getLog().warn("scala library version define in dependencies doesn't match the scalaVersion of the plugin");
                }
                getLog().info("suggestion: remove the scalaVersion from pom.xml");
            } else {
                scalaVersion = detectedScalaVersion;
            }
        }
        if (StringUtils.isEmpty(scalaVersion)) {
            throw new MojoFailureException("no scalaVersion detected or set");
        }
    }
    
    protected abstract void doExecute() throws Exception;

    protected JavaCommand getScalaCommand() throws Exception {
        JavaCommand cmd = getEmptyScalaCommand(scalaClassName);
        cmd.addArgs(args);
        cmd.addJvmArgs(jvmArgs);
        return cmd;
    }

    protected JavaCommand getEmptyScalaCommand(String mainClass) throws Exception {
        JavaCommand cmd = new JavaCommand(this, mainClass, getToolClasspath(), null, null);
        cmd.addJvmArgs("-Xbootclasspath/a:"+ getBootClasspath());
        return cmd;
    }

    private String getToolClasspath() throws Exception {
        Set<String> classpath = new HashSet<String>();
        addToClasspath(SCALA_GROUPID, "scala-compiler", scalaVersion, classpath);
//        addToClasspath(SCALA_GROUPID, "scala-decoder", scalaVersion, classpath);
//        addToClasspath(SCALA_GROUPID, "scala-dbc", scalaVersion, classpath);
        if (dependencies != null) {
            for(BasicArtifact artifact: dependencies) {
                addToClasspath(artifact.groupId, artifact.artifactId, artifact.version, classpath);
            }
        }
        return JavaCommand.toMultiPath(classpath.toArray(new String[classpath.size()]));
    }

    private String getBootClasspath() throws Exception {
        Set<String> classpath = new HashSet<String>();
        addToClasspath(SCALA_GROUPID, SCALA_LIBRARY_ARTIFACTID, scalaVersion, classpath);
        return JavaCommand.toMultiPath(classpath.toArray(new String[classpath.size()]));
    }

	/**
     * @return
     *           This returns whether or not the scala version can support having java sent into the compiler
     */
	protected boolean isJavaSupportedByCompiler() {
		return new VersionNumber(scalaVersion).compareTo(new VersionNumber("2.7.2")) >= 0;
	}

}

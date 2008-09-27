package org.scala_tools.maven.model;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.model.Build;
import org.apache.maven.model.CiManagement;
import org.apache.maven.model.Contributor;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Developer;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.IssueManagement;
import org.apache.maven.model.License;
import org.apache.maven.model.MailingList;
import org.apache.maven.model.Model;
import org.apache.maven.model.Organization;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Prerequisites;
import org.apache.maven.model.Profile;
import org.apache.maven.model.Reporting;
import org.apache.maven.model.Repository;
import org.apache.maven.model.Resource;
import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public class MavenProjectAdapter {

	final MavenProject wrapped;

	public MavenProjectAdapter(MavenProject project) {
		this.wrapped = project;
	}

	@SuppressWarnings("unchecked")
	public List<Profile> getActiveProfiles() {
		return wrapped.getActiveProfiles();
	}

	@SuppressWarnings("unchecked")
	public Map<String, Artifact> getArtifactMap() {
		return wrapped.getArtifactMap();
	}

	@SuppressWarnings("unchecked")
	public Set<Artifact> getArtifacts() {
		return wrapped.getArtifacts();
	}

	@SuppressWarnings("unchecked")
	public List<Artifact> getAttachedArtifacts() {
		return wrapped.getAttachedArtifacts();
	}

	@SuppressWarnings("unchecked")
	public List<Plugin> getBuildPlugins() {
		return wrapped.getBuildPlugins();
	}

	@SuppressWarnings("unchecked")
	public List<MavenProject> getCollectedProjects() {
		// TODO Auto-generated method stub
		return wrapped.getCollectedProjects();
	}

	@SuppressWarnings("unchecked")
	public List<Artifact> getCompileArtifacts() {
		return wrapped.getCompileArtifacts();
	}

	@SuppressWarnings("unchecked")
	public List<String> getCompileClasspathElements()
			throws DependencyResolutionRequiredException {
		return wrapped.getCompileClasspathElements();
	}

	@SuppressWarnings("unchecked")
	public List<Dependency> getCompileDependencies() {
		return wrapped.getCompileDependencies();
	}

	@SuppressWarnings("unchecked")
	public List<String> getCompileSourceRoots() {
		return wrapped.getCompileSourceRoots();
	}

	@SuppressWarnings("unchecked")
	public List<Contributor> getContributors() {
		return wrapped.getContributors();
	}

	@SuppressWarnings("unchecked")
	public List<Dependency> getDependencies() {
		return wrapped.getDependencies();
	}

	@SuppressWarnings("unchecked")
	public Set<Artifact> getDependencyArtifacts() {
		return wrapped.getDependencyArtifacts();
	}

	@SuppressWarnings("unchecked")
	public List<Developer> getDevelopers() {
		return wrapped.getDevelopers();
	}

	@SuppressWarnings("unchecked")
	public Map<String, Artifact> getExtensionArtifactMap() {
		return wrapped.getExtensionArtifactMap();
	}

	@SuppressWarnings("unchecked")
	public Set<Artifact> getExtensionArtifacts() {
		return wrapped.getExtensionArtifacts();
	}

	@SuppressWarnings("unchecked")
	public List<String> getFilters() {
		return wrapped.getFilters();
	}

	@SuppressWarnings("unchecked")
	public List<License> getLicenses() {
		return wrapped.getLicenses();
	}

	@SuppressWarnings("unchecked")
	public List<MailingList> getMailingLists() {
		return wrapped.getMailingLists();
	}

	@SuppressWarnings("unchecked")
	public Map getManagedVersionMap() {
		// TODO Figure out what is here
		return wrapped.getManagedVersionMap();
	}

	@SuppressWarnings("unchecked")
	public List<String> getModules() {
		return wrapped.getModules();
	}

	@SuppressWarnings("unchecked")
	public Map<String, Artifact> getPluginArtifactMap() {
		return wrapped.getPluginArtifactMap();
	}

	@SuppressWarnings("unchecked")
	public List<ArtifactRepository> getPluginArtifactRepositories() {
		return wrapped.getPluginArtifactRepositories();
	}

	@SuppressWarnings("unchecked")
	public Set<Artifact> getPluginArtifacts() {
		return wrapped.getPluginArtifacts();
	}

	@SuppressWarnings("unchecked")
	public List<Repository> getPluginRepositories() {
		return wrapped.getPluginRepositories();
	}

	@SuppressWarnings("unchecked")
	public Map<String, MavenProject> getProjectReferences() {
		return wrapped.getProjectReferences();
	}

	@SuppressWarnings("unchecked")
	public List<ArtifactRepository> getRemoteArtifactRepositories() {
		return wrapped.getRemoteArtifactRepositories();
	}

	@SuppressWarnings("unchecked")
	public Map<String, Artifact> getReportArtifactMap() {
		return wrapped.getReportArtifactMap();
	}

	@SuppressWarnings("unchecked")
	public Set<Artifact> getReportArtifacts() {
		return wrapped.getReportArtifacts();
	}

	@SuppressWarnings("unchecked")
	public List<Plugin> getReportPlugins() {
		return wrapped.getReportPlugins();
	}

	@SuppressWarnings("unchecked")
	public List<Repository> getRepositories() {
		return wrapped.getRepositories();
	}

	@SuppressWarnings("unchecked")
	public List<Resource> getResources() {
		return wrapped.getResources();
	}

	@SuppressWarnings("unchecked")
	public List<Artifact> getRuntimeArtifacts() {
		return wrapped.getRuntimeArtifacts();
	}

	@SuppressWarnings("unchecked")
	public List<String> getRuntimeClasspathElements()
			throws DependencyResolutionRequiredException {
		return wrapped.getRuntimeClasspathElements();
	}

	@SuppressWarnings("unchecked")
	public List<Dependency> getRuntimeDependencies() {
		return wrapped.getRuntimeDependencies();
	}

	@SuppressWarnings("unchecked")
	public List<String> getScriptSourceRoots() {
		return wrapped.getScriptSourceRoots();
	}

	@SuppressWarnings("unchecked")
	public List<Artifact> getSystemArtifacts() {
		return wrapped.getSystemArtifacts();
	}

	@SuppressWarnings("unchecked")
	public List<String> getSystemClasspathElements()
			throws DependencyResolutionRequiredException {
		return wrapped.getSystemClasspathElements();
	}

	@SuppressWarnings("unchecked")
	public List<Dependency> getSystemDependencies() {
		return wrapped.getSystemDependencies();
	}

	@SuppressWarnings("unchecked")
	public List<Artifact> getTestArtifacts() {
		return wrapped.getTestArtifacts();
	}

	@SuppressWarnings("unchecked")
	public List<String> getTestClasspathElements()
			throws DependencyResolutionRequiredException {
		return wrapped.getTestClasspathElements();
	}

	@SuppressWarnings("unchecked")
	public List<String> getTestCompileSourceRoots() {
		return wrapped.getTestCompileSourceRoots();
	}

	@SuppressWarnings("unchecked")
	public List<Dependency> getTestDependencies() {
		return wrapped.getTestDependencies();
	}

	@SuppressWarnings("unchecked")
	public List<Resource> getTestResources() {
		return wrapped.getTestResources();
	}

	/**
	 * Returns the property identified by the string. Multiple objects are
	 * checked to resolve the property:
	 * <ol>
	 * <li>The system properties (System.getProperty(key))</li>
	 * <li>The environment properties (System.getenv(key))</li>
	 * <li>The project properties (project.getProperty(key))</li>
	 * <li>
	 * The "standard" properties that one can reference in the pom. IE
	 * artifactId, build.directory, etc... Note: If the variable starts with
	 * project it may be dropped It is recommended that instead of using this
	 * method that you use get... (getArtifactId(),
	 * getBuild().getBuildDirectory)</li>
	 * </ol>
	 * first checked, then Environment variables, then the Project properties
	 * 
	 * @param key
	 * @return
	 */
	public String apply(String key) {
		if (key == null) {
			return null;
		}
		if (System.getProperty(key) != null) {
			return System.getProperty(key);
		}
		if (System.getenv(key) != null) {
			return System.getenv(key);
		}
		if (key.equals("build.directory")
				|| key.equals("project.build.directory")
				|| key.equals("buildDirectory")
				|| key.equals("project.buildDirectory")) {
			return getBuild().getDirectory();
		}
		if (key.equals("outputDirectory")
				|| key.equals("project.outputDirectory")
				|| key.equals("output.directory")
				|| key.equals("project.output.directory"))
			return getBuild().getOutputDirectory();

		if (key.equals("artifactId") || key.equals("project.artifactId")) {
			return getArtifactId();
		}

		if (key.equals("basedir") || key.equals("project.basedir"))
			return getBasedir().getAbsolutePath();

		if (key.equals("defaultGoal") || key.equals("project.defaultGoal"))
			return getBuild().getDefaultGoal();

		if (key.equals("finalName") || key.equals("project.finalName"))
			return getBuild().getFinalName();
		if (key.equals("scriptSourceDirectory")
				|| key.equals("project.scriptSourceDirectory")
				|| key.equals("script.source.directory")
				|| key.equals("project.script.source.directory"))
			return getBuild().getScriptSourceDirectory();
		if (key.equals("source.directory")
				|| key.equals("project.source.directory")
				|| key.equals("sourceDirectory")
				|| key.equals("project.sourceDirectory"))
			return getBuild().getSourceDirectory();
		if (key.equals("test.output.directory")
				|| key.equals("project.test.output.directory")
				|| key.equals("testOutputDirectory")
				|| key.equals("project.testOutputDirectory"))
			return getBuild().getTestOutputDirectory();
		if (key.equals("test.source.directory")
				|| key.equals("project.test.source.directory")
				|| key.equals("testSourceDirectory")
				|| key.equals("project.testSourceDirectory"))
			return getBuild().getTestSourceDirectory();
		if (key.equals("directory") || key.equals("project.directory"))
			return getDescription();
		if (key.equals("pom"))
			return getFile().getAbsolutePath();
		if (key.equals("groupId") || key.equals("project.groupId"))
			return getGroupId();
		if (key.equals("id") || key.equals("project.id"))
			return getId();
		if (key.equals("inception") || key.equals("project.inception")
				|| key.equals("inceptionYear")
				|| key.equals("project.inceptionYear")
				|| key.equals("inception.year")
				|| key.equals("project.inception.year"))
			return getInceptionYear();
		if (key.equals("name") || key.equals("project.name"))
			return getName();
		if (key.equals("packaging") || key.equals("project.packaging"))
			return getModel().getPackaging();
		if (key.equals("url") || key.equals("project.url"))
			return getModel().getUrl();
		if (key.equals("version") || key.equals("project.version"))
			return getModel().getVersion();

		return wrapped.getProperties().getProperty(key);
	}

	public String apply(String key, String defaultValue) {
		String result = apply(key);
		if (result == null) {
			return defaultValue;
		} else {
			return result;
		}
	}

	public void update(String key, String value) {
		if (key.equals("build.directory")
				|| key.equals("project.build.directory")
				|| key.equals("buildDirectory")
				|| key.equals("project.buildDirectory")) {
			getBuild().setDirectory(value);
		} else if (key.equals("outputDirectory")
				|| key.equals("project.outputDirectory")
				|| key.equals("output.directory")
				|| key.equals("project.output.directory")) {
			getBuild().setOutputDirectory(value);
		} else if (key.equals("artifactId") || key.equals("project.artifactId")) {
			setArtifactId(value);
		} else if (key.equals("defaultGoal")
				|| key.equals("project.defaultGoal")) {
			getBuild().setDefaultGoal(value);
		} else if (key.equals("finalName") || key.equals("project.finalName")) {
			getBuild().setFinalName(value);
		} else if (key.equals("scriptSourceDirectory")
				|| key.equals("project.scriptSourceDirectory")
				|| key.equals("script.source.directory")
				|| key.equals("project.script.source.directory")) {
			getBuild().setScriptSourceDirectory(value);
		} else if (key.equals("source.directory")
				|| key.equals("project.source.directory")
				|| key.equals("sourceDirectory")
				|| key.equals("project.sourceDirectory")) {
			getBuild().setSourceDirectory(value);
		} else if (key.equals("test.output.directory")
				|| key.equals("project.test.output.directory")
				|| key.equals("testOutputDirectory")
				|| key.equals("project.testOutputDirectory")) {
			getBuild().setTestOutputDirectory(value);
		} else if (key.equals("test.source.directory")
				|| key.equals("project.test.source.directory")
				|| key.equals("testSourceDirectory")
				|| key.equals("project.testSourceDirectory")) {
			getBuild().setTestSourceDirectory(value);
		} else if (key.equals("directory") || key.equals("project.directory")) {
			setDescription(value);
		} else if (key.equals("pom")) {
			setFile(new File(value));
		} else if (key.equals("groupId") || key.equals("project.groupId")) {
			setGroupId(value);
		} else if (key.equals("inception") || key.equals("project.inception")
				|| key.equals("inceptionYear")
				|| key.equals("project.inceptionYear")
				|| key.equals("inception.year")
				|| key.equals("project.inception.year")) {
			setInceptionYear(value);
		} else if (key.equals("name") || key.equals("project.name")) {
			setName(value);
		} else if (key.equals("packaging") || key.equals("project.packaging")) {
			getModel().setPackaging(value);
		} else if (key.equals("url") || key.equals("project.url")) {
			getModel().setUrl(value);
		} else if (key.equals("version") || key.equals("project.version")) {
			getModel().setVersion(value);
		} else {
			wrapped.getProperties().setProperty(key, value);
		}
	}

	public void update(String key, int value) {
		update(key, String.valueOf(value));
	}

	public void update(String key, boolean value) {
		update(key, String.valueOf(value));
	}

	public void update(String key, double value) {
		update(key, String.valueOf(value));
	}

	public void update(String key, long value) {
		update(key, String.valueOf(value));
	}

	public void update(String key, char value) {
		update(key, String.valueOf(value));
	}

	public void update(String key, float value) {
		update(key, String.valueOf(value));
	}

	public void update(String key, byte value) {
		update(key, String.valueOf(value));
	}

	public void addAttachedArtifact(Artifact artifact) {
		wrapped.addAttachedArtifact(artifact);
	}

	public void addCompileSourceRoot(String path) {
		wrapped.addCompileSourceRoot(path);
	}

	public void addContributor(Contributor contributor) {
		wrapped.addContributor(contributor);
	}

	public void addDeveloper(Developer developer) {
		wrapped.addDeveloper(developer);
	}

	public void addLicense(License license) {
		wrapped.addLicense(license);
	}

	public void addMailingList(MailingList mailingList) {
		wrapped.addMailingList(mailingList);
	}

	public void addPlugin(Plugin plugin) {
		wrapped.addPlugin(plugin);
	}

	public void addProjectReference(MavenProject project) {
		wrapped.addProjectReference(project);
	}

	public void addResource(Resource resource) {
		wrapped.addResource(resource);
	}

	public void addScriptSourceRoot(String path) {
		wrapped.addScriptSourceRoot(path);
	}

	public void addTestCompileSourceRoot(String path) {
		wrapped.addTestCompileSourceRoot(path);
	}

	public void addTestResource(Resource testResource) {
		wrapped.addTestResource(testResource);
	}

	@SuppressWarnings("unchecked")
	public Set<Artifact> createArtifacts(ArtifactFactory artifactFactory,
			String inheritedScope, ArtifactFilter dependencyFilter)
			throws InvalidDependencyVersionException {
		return wrapped.createArtifacts(artifactFactory, inheritedScope,
				dependencyFilter);
	}

	public boolean equals(Object arg0) {
		return wrapped.equals(arg0);
	}

	public Artifact getArtifact() {
		return wrapped.getArtifact();
	}

	public String getArtifactId() {
		return wrapped.getArtifactId();
	}

	public File getBasedir() {
		return wrapped.getBasedir();
	}

	public Build getBuild() {
		return wrapped.getBuild();
	}

	public List getBuildExtensions() {
		return wrapped.getBuildExtensions();
	}

	public CiManagement getCiManagement() {
		return wrapped.getCiManagement();
	}

	public String getDefaultGoal() {
		return wrapped.getDefaultGoal();
	}

	public DependencyManagement getDependencyManagement() {
		return wrapped.getDependencyManagement();
	}

	public String getDescription() {
		return wrapped.getDescription();
	}

	public DistributionManagement getDistributionManagement() {
		return wrapped.getDistributionManagement();
	}

	public ArtifactRepository getDistributionManagementArtifactRepository() {
		return wrapped.getDistributionManagementArtifactRepository();
	}

	public MavenProject getExecutionProject() {
		return wrapped.getExecutionProject();
	}

	public File getFile() {
		return wrapped.getFile();
	}

	public Xpp3Dom getGoalConfiguration(String arg0, String arg1, String arg2,
			String arg3) {
		return wrapped.getGoalConfiguration(arg0, arg1, arg2, arg3);
	}

	public String getGroupId() {
		return wrapped.getGroupId();
	}

	public String getId() {
		return wrapped.getId();
	}

	public String getInceptionYear() {
		return wrapped.getInceptionYear();
	}

	public IssueManagement getIssueManagement() {
		return wrapped.getIssueManagement();
	}

	public Model getModel() {
		return wrapped.getModel();
	}

	public String getModelVersion() {
		return wrapped.getModelVersion();
	}

	public String getModulePathAdjustment(MavenProject arg0) throws IOException {
		return wrapped.getModulePathAdjustment(arg0);
	}

	public String getName() {
		return wrapped.getName();
	}

	public Organization getOrganization() {
		return wrapped.getOrganization();
	}

	public Model getOriginalModel() {
		return wrapped.getOriginalModel();
	}

	public String getPackaging() {
		return wrapped.getPackaging();
	}

	public MavenProject getParent() {
		return wrapped.getParent();
	}

	public Artifact getParentArtifact() {
		return wrapped.getParentArtifact();
	}

	public PluginManagement getPluginManagement() {
		return wrapped.getPluginManagement();
	}

	public Prerequisites getPrerequisites() {
		return wrapped.getPrerequisites();
	}

	public Properties getProperties() {
		return wrapped.getProperties();
	}

	public Xpp3Dom getReportConfiguration(String arg0, String arg1, String arg2) {
		return wrapped.getReportConfiguration(arg0, arg1, arg2);
	}

	public Reporting getReporting() {
		return wrapped.getReporting();
	}

	public Scm getScm() {
		return wrapped.getScm();
	}

	public String getUrl() {
		return wrapped.getUrl();
	}

	public String getVersion() {
		return wrapped.getVersion();
	}

	public int hashCode() {
		return wrapped.hashCode();
	}

	public boolean hasParent() {
		return wrapped.hasParent();
	}

	public void injectPluginManagementInfo(Plugin arg0) {
		wrapped.injectPluginManagementInfo(arg0);
	}

	public boolean isExecutionRoot() {
		return wrapped.isExecutionRoot();
	}

	public Artifact replaceWithActiveArtifact(Artifact arg0) {
		return wrapped.replaceWithActiveArtifact(arg0);
	}

	public void setActiveProfiles(List<Profile> activeProfiles) {
		wrapped.setActiveProfiles(activeProfiles);
	}

	public void setArtifact(Artifact artifact) {
		wrapped.setArtifact(artifact);
	}

	public void setArtifactId(String artifactId) {
		wrapped.setArtifactId(artifactId);
	}

	public void setArtifacts(Set<Artifact> artifacts) {
		wrapped.setArtifacts(artifacts);
	}

	public void setBuild(Build build) {
		wrapped.setBuild(build);
	}

	public void setCiManagement(CiManagement ciManagement) {
		wrapped.setCiManagement(ciManagement);
	}

	public void setCollectedProjects(List<MavenProject> collectedProjects) {
		wrapped.setCollectedProjects(collectedProjects);
	}

	public void setContributors(List<Contributor> contributors) {
		wrapped.setContributors(contributors);
	}

	public void setDependencies(List dependencies) {
		wrapped.setDependencies(dependencies);
	}

	public void setDependencyArtifacts(Set dependencyArtifacts) {
		wrapped.setDependencyArtifacts(dependencyArtifacts);
	}

	public void setDescription(String description) {
		wrapped.setDescription(description);
	}

	public void setDevelopers(List developers) {
		wrapped.setDevelopers(developers);
	}

	public void setDistributionManagement(
			DistributionManagement distributionManagement) {
		wrapped.setDistributionManagement(distributionManagement);
	}

	public void setExecutionProject(MavenProject executionProject) {
		wrapped.setExecutionProject(executionProject);
	}

	public void setExecutionRoot(boolean executionRoot) {
		wrapped.setExecutionRoot(executionRoot);
	}

	public void setExtensionArtifacts(Set extensionArtifacts) {
		wrapped.setExtensionArtifacts(extensionArtifacts);
	}

	public void setFile(File file) {
		wrapped.setFile(file);
	}

	public void setGroupId(String groupId) {
		wrapped.setGroupId(groupId);
	}

	public void setInceptionYear(String inceptionYear) {
		wrapped.setInceptionYear(inceptionYear);
	}

	public void setIssueManagement(IssueManagement issueManagement) {
		wrapped.setIssueManagement(issueManagement);
	}

	public void setLicenses(List licenses) {
		wrapped.setLicenses(licenses);
	}

	public void setMailingLists(List mailingLists) {
		wrapped.setMailingLists(mailingLists);
	}

	public void setManagedVersionMap(Map map) {
		wrapped.setManagedVersionMap(map);
	}

	public void setModelVersion(String pomVersion) {
		wrapped.setModelVersion(pomVersion);
	}

	public void setName(String name) {
		wrapped.setName(name);
	}

	public void setOrganization(Organization organization) {
		wrapped.setOrganization(organization);
	}

	public void setOriginalModel(Model originalModel) {
		wrapped.setOriginalModel(originalModel);
	}

	public void setPackaging(String packaging) {
		wrapped.setPackaging(packaging);
	}

	public void setParent(MavenProject parent) {
		wrapped.setParent(parent);
	}

	public void setParentArtifact(Artifact parentArtifact) {
		wrapped.setParentArtifact(parentArtifact);
	}

	public void setPluginArtifactRepositories(List pluginArtifactRepositories) {
		wrapped.setPluginArtifactRepositories(pluginArtifactRepositories);
	}

	public void setPluginArtifacts(Set pluginArtifacts) {
		wrapped.setPluginArtifacts(pluginArtifacts);
	}

	public void setReleaseArtifactRepository(
			ArtifactRepository releaseArtifactRepository) {
		wrapped.setReleaseArtifactRepository(releaseArtifactRepository);
	}

	public void setRemoteArtifactRepositories(List remoteArtifactRepositories) {
		wrapped.setRemoteArtifactRepositories(remoteArtifactRepositories);
	}

	public void setReportArtifacts(Set reportArtifacts) {
		wrapped.setReportArtifacts(reportArtifacts);
	}

	public void setReporting(Reporting reporting) {
		wrapped.setReporting(reporting);
	}

	public void setScm(Scm scm) {
		wrapped.setScm(scm);
	}

	public void setSnapshotArtifactRepository(
			ArtifactRepository snapshotArtifactRepository) {
		wrapped.setSnapshotArtifactRepository(snapshotArtifactRepository);
	}

	public void setUrl(String url) {
		wrapped.setUrl(url);
	}

	public void setVersion(String version) {
		wrapped.setVersion(version);
	}

	public String toString() {
		return wrapped.toString();
	}

	public void writeModel(Writer writer) throws IOException {
		wrapped.writeModel(writer);
	}

	public void writeOriginalModel(Writer writer) throws IOException {
		wrapped.writeOriginalModel(writer);
	}

	public MavenProject getWrapped() {
		return wrapped;
	}

}

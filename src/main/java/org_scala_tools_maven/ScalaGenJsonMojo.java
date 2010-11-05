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
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.License;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.plexus.util.StringUtils;

import edu.emory.mathcs.backport.java.util.Arrays;
import org_scala_tools_maven_executions.JavaMainCaller;
import org_scala_tools_maven_executions.JavaMainCallerByFork;
import org_scala_tools_maven_executions.MainHelper;

/**
 * Produces Scala API documentation in Json (use vscaladoc2_genjson).
 * 
 * @goal genjson
 * @requiresDependencyResolution compile
 * @execute phase="generate-sources"
 * @since 2.15.0
 */
public class ScalaGenJsonMojo extends ScalaSourceMojoSupport {

  /**
   * Define the html fragment for logo.
   * 
   * @parameter expression="${logo}"
   *            default-value="<a href='${project.url}'>${project.name}</a>"
   */
  protected String logo;

  /**
   * Define the html fragment for license (default : use info of the first entry of pom.xml/project/licenses).
   * 
   * @parameter expression="${license}" 
   */
  protected String license;

  /**
   * Define the artifact's tags (space separator).
   * 
   * @parameter expression="${tags}" default-value=""
   */
  protected String tags = "";

  // /**
  // * Generate source in HTML (not yet supported by vscaladoc2)
  // *
  // *
  // * @parameter expression="${linksource}" default-value="true"
  // */
  // protected boolean linksource;

  /**
   * Define the html fragment for description (use in overview page).
   * 
   * @parameter expression="${description}"
   *            default-value="${project.description}"
   */
  private String description;

  /**
   * Define the version of vscaladoc2_genjson to use.
   * 
   * @parameter expression="${vscaladoc2_genjson.version}"
   *            default-value="0.2-SNAPSHOT"
   */
  protected String vscaladoc2Version;

  /**
   * The directory which contains scala/java source files
   * 
   * @parameter expression="${project.build.sourceDirectory}/../scala"
   */
  protected File sourceDir;

  protected String _mainClass = "net_alchim31_vscaladoc2_genjson.Main";
  protected boolean _prettyPrint = true;

  public ScalaGenJsonMojo() throws Exception {
    super();
  }

  @Override
  @SuppressWarnings("unchecked")
  protected List<File> getSourceDirectories() throws Exception {
    List<String> sources = project.getCompileSourceRoots();
    // Quick fix in case the user has not added the "add-source" goal.
    String scalaSourceDir = sourceDir.getCanonicalPath();
    if (!sources.contains(scalaSourceDir)) {
      sources.add(scalaSourceDir);
    }
    return normalize(sources);
  }

  @Override
  protected void doExecute() throws Exception {
    if (StringUtils.isNotEmpty(_mainClass)) {
      File cfg = makeJsonCfg();
      setDependenciesForJcmd();
      JavaMainCaller jcmd = getEmptyScalaCommand(_mainClass);
      jcmd.addJvmArgs(jvmArgs);
      jcmd.addArgs(cfg.getCanonicalPath());
      jcmd.run(displayCmd);
    } else {
      getLog().warn("Not mainClass or valid launcher found/define");
    }
  }

  private void setDependenciesForJcmd() {
    BasicArtifact artifact = new BasicArtifact();
    artifact.artifactId = "vscaladoc2_genjson";
    artifact.groupId = "net.alchim31.vscaladoc2";
    artifact.version = vscaladoc2Version;
    dependencies = new BasicArtifact[] { artifact };
  }

  private void toJson(Object pojo, boolean prettyPrint, File f) throws Exception {
    ObjectMapper m = new ObjectMapper();
    JsonFactory jf = new JsonFactory();
    JsonGenerator jg = jf.createJsonGenerator(f, JsonEncoding.UTF8);
    if (prettyPrint) {
      jg.useDefaultPrettyPrinter();
    }
    m.writeValue(jg, pojo);
  }

  private File makeJsonCfg() throws Exception {
    initFilters();
    File dir = new File(project.getBuild().getDirectory());
    dir.mkdirs();
    File f = new File(dir, "vscaladoc2_cfg.json");
    toJson(new Cfg(this), _prettyPrint, f);
    return f;
  }

  protected static class Cfg {
    public String groupId = "";
    public String artifactId = "undef";
    public String version = "0.0.0";
    public String description = "";
    public String logo = "";
    public String license = "";
    public String kind = "";
    public String tags = "";
    public List<List<String>> dependencies = Collections.emptyList();
    public List<List<Object>> sources = Collections.emptyList();
    public List<String> artifacts = Collections.emptyList();
    public List<String> additionnalArgs = Collections.emptyList();

    @SuppressWarnings("unchecked")
    protected Cfg(ScalaGenJsonMojo data) throws Exception {
      groupId = data.project.getGroupId();
      artifactId = data.project.getArtifactId();
      version = data.project.getVersion();
      logo = data.logo;
      license = data.license;
      description = data.description;
      tags = data.tags;
      if (StringUtils.isBlank(license) && !data.project.getLicenses().isEmpty()) {
        License lic = (License) data.project.getLicenses().get(0); 
        license = String.format("<a href='%s'>%s</a>", lic.getUrl(), lic.getName());
      }
      dependencies = makeDependencies(data);
      sources = makeSources(data);
      artifacts = makeArtifacts(data);
      if (data.args != null && data.args.length > 0) {
        additionnalArgs = Arrays.asList(data.args);
      }
      kind = makeKind(data);
    }

    private String makeKind(ScalaGenJsonMojo data) {
      String back = null;
      String pkg = data.project.getPackaging();
      if ("pom".equals(pkg)) {
        back = "group";
      } else {
        back = pkg;
      }
      return back;
    }

    protected List<List<String>> makeDependencies(ScalaGenJsonMojo data) throws Exception {
      List<List<String>> back = new ArrayList<List<String>>();
      @SuppressWarnings("unchecked")
      List<Artifact> deps = data.project.getCompileArtifacts();
      for (Artifact dep : deps) {
        List<String> e = new ArrayList<String>(3);
        e.add(dep.getFile().getCanonicalPath());
        e.add(dep.getArtifactId() + "/" + dep.getVersion());
        back.add(e);
      }
      return back;
    }

    protected List<List<Object>> makeSources(ScalaGenJsonMojo data) throws Exception {
      List<List<Object>> back = new ArrayList<List<Object>>();
      List<File> dirs = data.getSourceDirectories();
      List<String> includes = new ArrayList<String>(data.includes);
      List<String> excludes = new ArrayList<String>(data.excludes);
      for (File dir : dirs) {
        List<Object> e = new ArrayList<Object>(3);
        e.add(dir.getCanonicalPath());
        e.add(excludes);
        e.add(includes);
        back.add(e);
      }
      return back;
    }

    protected List<String> makeArtifacts(ScalaGenJsonMojo data) throws Exception {
      List<String> back = new ArrayList<String>();
      @SuppressWarnings("unchecked")
      List<MavenProject> modules = data.project.getCollectedProjects();
      for (MavenProject module : modules) {
        back.add(module.getArtifactId() + "/" + module.getVersion());
      }
      return back;
    }
  }
}

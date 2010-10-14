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
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.plexus.util.StringUtils;
import org_scala_tools_maven_executions.JavaMainCaller;
import org_scala_tools_maven_executions.MainHelper;

/**
 * Produces Scala API documentation.
 *
 * @goal doc
 * @requiresDependencyResolution compile
 * @execute phase="generate-sources"
 */
public class ScalaDocMojo extends ScalaSourceMojoSupport implements MavenReport {

    /**
     * Specify window title of generated HTML documentation.
     * [scaladoc, vscaladoc]
     *
     * @parameter expression="${windowtitle}"
     *            default-value="${project.name} ${project.version} API"
     */
    protected String windowtitle;

    /**
     * Specifies the text to be placed at the bottom of each output file. If you
     * want to use html you have to put it in a CDATA section, eg.
     * &lt;![CDATA[Copyright 2005, &lt;a
     * href="http://www.mycompany.com">MyCompany, Inc.&lt;a>]]&gt;
     * [scaladoc, vscaladoc]
     *
     * @parameter expression="${bottom}"
     *            default-value="Copyright (c) {inceptionYear}-{currentYear} {organizationName}. All Rights Reserved."
     */
    protected String bottom;

    /**
     * Charset for cross-platform viewing of generated documentation.
     * [scaladoc, vscaladoc]
     *
     * @parameter expression="${charset}" default-value="ISO-8859-1"
     */
    protected String charset;

    /**
     * Include title for the overview page.
     * [scaladoc, scaladoc2, vscaladoc]
     *
     * @parameter expression="${doctitle}"
     *            default-value="${project.name} ${project.version} API"
     */
    protected String doctitle;

    /**
     * Include footer text for each page.
     * [scaladoc, vscaladoc]
     *
     * @parameter expression="${footer}"
     */
    protected String footer;

    /**
     * Include header text for each page
     * [scaladoc, vscaladoc]
     *
     * @parameter expression="${header}"
     */
    protected String header;

    /**
     * Generate source in HTML
     * [scaladoc, vscaladoc]
     *
     * @parameter expression="${linksource}" default-value="true"
     */
    protected boolean linksource;

    /**
     * Suppress description and tags, generate only declarations
     * [scaladoc, vscaladoc]
     *
     * @parameter expression="${nocomment}" default-value="false"
     */
    protected boolean nocomment;

    /**
     * File to change style of the generated documentation
     * [scaladoc, vscaladoc]
     *
     * @parameter expression="${stylesheetfile}"
     */
    protected File stylesheetfile;

    /**
     * Include top text for each page
     * [scaladoc, vscaladoc]
     *
     * @parameter expression="${top}"
     */
    protected String top;

    /**
     * Specifies the destination directory where scalaDoc saves the generated
     * HTML files.
     *
     * @parameter expression="scaladocs"
     * @required
     */
    private String outputDirectory;

    /**
     * Specifies the destination directory where javadoc saves the generated HTML files.
     *
     * @parameter expression="${project.reporting.outputDirectory}/scaladocs"
     * @required
     */
    private File reportOutputDirectory;

    /**
     * The name of the Scaladoc report.
     *
     * @since 2.1
     * @parameter expression="${name}" default-value="ScalaDocs"
     */
    private String name;

    /**
     * The description of the Scaladoc report.
     *
     * @since 2.1
     * @parameter expression="${description}" default-value="ScalaDoc API
     *            documentation."
     */
    private String description;

    /**
     * className (FQN) of the main scaladoc to use, if not define, the the scalaClassName is used
     *
     * @parameter expression="${maven.scaladoc.className}"
     */
    protected String scaladocClassName;

    /**
     * If you want to use vscaladoc to generate api instead of regular scaladoc, set the version of vscaladoc you want to use.
     *
     * @parameter expression="${maven.scaladoc.vscaladocVersion}"
     */
    protected String vscaladocVersion;

    /**
     * To allow running aggregation only from command line use "-Dforce-aggregate=true" (avoid using in pom.xml).
     * [scaladoc, vscaladoc]
     *
     * @parameter expression="${force-aggregate}" default-value="false"
     */
    protected boolean forceAggregate = false;

    /**
     * If you want to aggregate only direct sub modules.
     *
     * @parameter expression="${maven.scaladoc.aggregateDirectOnly}" default-value="true"
     */
    protected boolean aggregateDirectOnly = true;

    /**
     * The directory which contains scala/java source files
     *
     * @parameter expression="${project.build.sourceDirectory}/../scala"
     */
    protected File sourceDir;

    private List<File> _sourceFiles;

    @Override
    @SuppressWarnings("unchecked")
    protected List<File> getSourceDirectories() throws Exception {
        List<String> sources = project.getCompileSourceRoots();
        //Quick fix in case the user has not added the "add-source" goal.
        String scalaSourceDir = sourceDir.getCanonicalPath();
        if(!sources.contains(scalaSourceDir)) {
            sources.add(scalaSourceDir);
        }
        return normalize(sources);
    }

    public boolean canGenerateReport() {
        // there is modules to aggregate
        boolean back = ((project.isExecutionRoot() || forceAggregate) && canAggregate() && project.getCollectedProjects().size() > 0);
        back = back || (findSourceFiles().size() != 0);
        return back;
    }

    /**
     * @return
     * @throws Exception
     */
    private List<File> findSourceFiles() {
        if (_sourceFiles == null) {
            try {
                _sourceFiles = findSourceWithFilters();
            } catch (Exception exc) {
                throw new RuntimeException("can't define source to process", exc);
            }
        }
        return _sourceFiles;
    }

    private boolean canAggregate() {
        return StringUtils.isNotEmpty(vscaladocVersion) && (new VersionNumber(vscaladocVersion).compareTo(new VersionNumber("1.1")) >= 0);
    }

    public boolean isExternalReport() {
        return true;
    }

    public String getCategoryName() {
        return CATEGORY_PROJECT_REPORTS;
    }

    public String getDescription(@SuppressWarnings("unused") Locale locale) {
        if (StringUtils.isEmpty(description)) {
            return "ScalaDoc API documentation";
        }
        return description;
    }

    public String getName(@SuppressWarnings("unused") Locale locale) {
        if (StringUtils.isEmpty(name)) {
            return "ScalaDocs";
        }
        return name;
    }

    public String getOutputName() {
        return outputDirectory + "/index";
    }

    public File getReportOutputDirectory() {
        if (reportOutputDirectory == null) {
            reportOutputDirectory = new File(project.getBasedir(), project.getReporting().getOutputDirectory() + "/" + outputDirectory).getAbsoluteFile();
        }
        return reportOutputDirectory;
    }

    public void setReportOutputDirectory(File v) {
        if (v != null && outputDirectory != null && !v.getAbsolutePath().endsWith(outputDirectory)) {
            this.reportOutputDirectory = new File(v, outputDirectory);
        } else {
            this.reportOutputDirectory = v;
        }
    }

    @Override
    public void doExecute() throws Exception {
        // SiteRendererSink sink = siteRenderer.createSink(new
        // File(project.getReporting().getOutputDirectory(), getOutputName() +
        // ".html");
        generate(null, Locale.getDefault());
    }


    @SuppressWarnings("unchecked")
    @Override
    protected JavaMainCaller getScalaCommand() throws Exception {
        //This ensures we have a valid scala version...
        checkScalaVersion();
        boolean isPreviousScala271 = (new VersionNumber("2.7.1").compareTo(findScalaVersion()) > 0);
        if (StringUtils.isEmpty(scaladocClassName)) {
            if (!isPreviousScala271) {
                scaladocClassName = "scala.tools.nsc.ScalaDoc";
            } else {
                scaladocClassName = scalaClassName;
            }
        }

        JavaMainCaller jcmd = getEmptyScalaCommand(scaladocClassName);
        jcmd.addArgs(args);
        jcmd.addJvmArgs(jvmArgs);

        if (isPreviousScala271){
            jcmd.addArgs("-Ydoc");
        }
        List<String> paths = project.getCompileClasspathElements();
        paths.remove(project.getBuild().getOutputDirectory()); //remove output to avoid "error for" : error:  XXX is already defined as package XXX ... object XXX {
        jcmd.addOption("-classpath", MainHelper.toMultiPath(paths));
        //jcmd.addOption("-sourcepath", sourceDir.getAbsolutePath());

        boolean isScaladoc2 = (new VersionNumber("2.8.0").compareTo(findScalaVersion()) <= 0) && ("scala.tools.nsc.ScalaDoc".equals(scaladocClassName));
        if (isScaladoc2) {
            jcmd.addArgs("-doc-format:html");
            jcmd.addOption("-doc-title", doctitle);
        } else {
            jcmd.addOption("-bottom", getBottomText());
            jcmd.addOption("-charset", charset);
            jcmd.addOption("-doctitle", doctitle);
            jcmd.addOption("-footer", footer);
            jcmd.addOption("-header", header);
            jcmd.addOption("-linksource", linksource);
            jcmd.addOption("-nocomment", nocomment);
            jcmd.addOption("-stylesheetfile", stylesheetfile);
            jcmd.addOption("-top", top);
            jcmd.addOption("-windowtitle", windowtitle);
        }
        return jcmd;
    }

    public void generate(@SuppressWarnings("unused") Sink sink, @SuppressWarnings("unused") Locale locale) throws MavenReportException {
        try {
            if (!canGenerateReport()) {
                getLog().warn("No source files found");
                return;
            }

            File reportOutputDir = getReportOutputDirectory();
            if (!reportOutputDir.exists()) {
                reportOutputDir.mkdirs();
            }
            if (StringUtils.isNotEmpty(vscaladocVersion)) {
                scaladocClassName = "org.scala_tools.vscaladoc.Main";
                BasicArtifact artifact = new BasicArtifact();
                artifact.artifactId = "vscaladoc";
                artifact.groupId = "org.scala-tools";
                artifact.version = vscaladocVersion;
                dependencies = new BasicArtifact[]{artifact};
            }

            List<File> sources = findSourceFiles();
            if (sources.size() > 0) {
                JavaMainCaller jcmd = getScalaCommand();
                jcmd.addOption("-d", reportOutputDir.getAbsolutePath());
                for (File x : sources) {
                    jcmd.addArgs(x.getCanonicalPath());
                }
                jcmd.run(displayCmd);
            }
            if (forceAggregate) {
                aggregate(project);
            } else {
                // Mojo could not be run from parent after all its children
                // So the aggregation will be run after the last child
                tryAggregateUpper(project);
            }

        } catch (MavenReportException exc) {
            throw exc;
        } catch (RuntimeException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new MavenReportException("wrap: " + exc.getMessage(), exc);
        }
    }

    @SuppressWarnings("unchecked")
    protected void tryAggregateUpper(MavenProject prj) throws Exception {
        if (prj != null && prj.hasParent() && canAggregate()) {
            MavenProject parent = prj.getParent();
            List<MavenProject> modules = parent.getCollectedProjects();
            if ((modules.size() > 1) && prj.equals(modules.get(modules.size() - 1))) {
                aggregate(parent);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void aggregate(MavenProject parent) throws Exception {
        List<MavenProject> modules = parent.getCollectedProjects();
        File dest = new File(parent.getReporting().getOutputDirectory() +"/" + outputDirectory);
        getLog().info("start aggregation into " + dest);
        StringBuilder mpath = new StringBuilder();
        for (MavenProject module : modules) {
            if ( "pom".equals( module.getPackaging().toLowerCase() ) ) {
                continue;
            }
            if (aggregateDirectOnly && module.getParent() != parent) {
                continue;
            }
            File subScaladocPath = new File(module.getReporting().getOutputDirectory() +"/" + outputDirectory).getAbsoluteFile();
            //System.out.println(" -> " + project.getModulePathAdjustment(module)  +" // " + subScaladocPath + " // " + module.getBasedir() );
            if (subScaladocPath.exists()) {
                mpath.append(subScaladocPath).append(File.pathSeparatorChar);
            }
        }
        if (mpath.length() != 0) {
            getLog().info("aggregate vscaladoc from : " + mpath);
            JavaMainCaller jcmd = getScalaCommand();
            jcmd.addOption("-d", dest.getAbsolutePath());
            jcmd.addOption("-aggregate", mpath.toString());
            jcmd.run(displayCmd);
        } else {
            getLog().warn("no vscaladoc to aggregate");
        }
        tryAggregateUpper(parent);
    }

    /**
     * Method that sets the bottom text that will be displayed on the bottom of
     * the javadocs.
     *
     * @param inceptionYear the year when the project was started
     * @return a String that contains the text that will be displayed at the
     *         bottom of the javadoc
     */
    private String getBottomText() {
        String inceptionYear = project.getInceptionYear();
        int actualYear = Calendar.getInstance().get(Calendar.YEAR);
        String year = String.valueOf(actualYear);

        String theBottom = StringUtils.replace(bottom, "{currentYear}", year);

        if (inceptionYear != null) {
            if (inceptionYear.equals(year)) {
                theBottom = StringUtils.replace(theBottom, "{inceptionYear}-", "");
            } else {
                theBottom = StringUtils.replace(theBottom, "{inceptionYear}", inceptionYear);
            }
        } else {
            theBottom = StringUtils.replace(theBottom, "{inceptionYear}-", "");
        }

        if (project.getOrganization() == null) {
            theBottom = StringUtils.replace(theBottom, " {organizationName}", "");
        } else {
            if ((project.getOrganization() != null) && (StringUtils.isNotEmpty(project.getOrganization().getName()))) {
                if (StringUtils.isNotEmpty(project.getOrganization().getUrl())) {
                    theBottom = StringUtils.replace(theBottom, "{organizationName}", "<a href=\"" + project.getOrganization().getUrl() + "\">" + project.getOrganization().getName() + "</a>");
                } else {
                    theBottom = StringUtils.replace(theBottom, "{organizationName}", project.getOrganization().getName());
                }
            } else {
                theBottom = StringUtils.replace(theBottom, " {organizationName}", "");
            }
        }

        return theBottom;
    }
}

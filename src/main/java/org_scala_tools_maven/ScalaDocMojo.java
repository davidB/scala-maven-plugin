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
import java.io.IOException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
public class ScalaDocMojo extends ScalaMojoSupport implements MavenReport {
    /**
     * Specify window title of generated HTML documentation.
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
     *
     * @parameter expression="${bottom}"
     *            default-value="Copyright (c) {inceptionYear}-{currentYear} {organizationName}. All Rights Reserved."
     */
    protected String bottom;

    /**
     * Charset for cross-platform viewing of generated documentation.
     *
     * @parameter expression="${charset}" default-value="ISO-8859-1"
     */
    protected String charset;

    /**
     * Include title for the overview page.
     *
     * @parameter expression="${doctitle}"
     *            default-value="${project.name} ${project.version} API"
     */
    protected String doctitle;

    /**
     * Include footer text for each page.
     *
     * @parameter expression="${footer}"
     */
    protected String footer;

    /**
     * Include header text for each page
     *
     * @parameter expression="${header}"
     */
    protected String header;

    /**
     * Generate source in HTML
     *
     * @parameter expression="${linksource}" default-value="true"
     */
    protected boolean linksource;

    /**
     * Suppress description and tags, generate only declarations
     *
     * @parameter expression="${nocomment}" default-value="false"
     */
    protected boolean nocomment;

    /**
     * File to change style of the generated documentation
     *
     * @parameter expression="${stylesheetfile}"
     */
    protected File stylesheetfile;

    /**
     * Include top text for each page
     *
     * @parameter expression="${top}"
     */
    protected String top;

    /**
     * The directory in which to find scala source
     *
     * @parameter expression="${project.build.sourceDirectory}/../scala"
     */
    protected File sourceDir;

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

    private String[] sourceFiles_ = null;

    /**
     * A list of inclusion filters for the compiler.
     *
     * @parameter
     */
    private Set<String> includes = new HashSet<String>();

    /**
     * A list of exclusion filters for the compiler.
     *
     * @parameter
     */
    private Set<String> excludes = new HashSet<String>();


    private String[] findSourceFiles() {
        if (sourceFiles_ == null) {
            if(includes.isEmpty()) {
                includes.add("**/*.scala");
            }
            sourceFiles_ = MainHelper.findFiles(sourceDir, includes.toArray(new String[includes.size()]), excludes.toArray(new String[excludes.size()]));
        }
        return sourceFiles_;
    }

    public boolean canGenerateReport() {
        try {
            sourceDir = sourceDir.getCanonicalFile();
        } catch (IOException exc) {
            sourceDir = sourceDir.getAbsoluteFile();
        }
        // there is source to compile
        boolean back = sourceDir.exists() && (findSourceFiles().length != 0);
        // there is modules to aggregate
        back = back || ((project.isExecutionRoot() || forceAggregate) && canAggregate() && project.getCollectedProjects().size() > 0);
        return back;
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

    public void setReportOutputDirectory(File reportOutputDirectory) {
        if (reportOutputDirectory != null && !reportOutputDirectory.getAbsolutePath().endsWith(outputDirectory)) {
            this.reportOutputDirectory = new File(reportOutputDirectory, outputDirectory);
        }
        else {
            this.reportOutputDirectory = reportOutputDirectory;
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
        boolean isPreviousScala271 = (new VersionNumber("2.7.1").compareTo(new VersionNumber(scalaVersion)) > 0);
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
        jcmd.addOption("-sourcepath", sourceDir.getAbsolutePath());

        boolean isScaladoc2 = (new VersionNumber("2.8.0").compareTo(new VersionNumber(scalaVersion)) <= 0) && ("scala.tools.nsc.ScalaDoc".equals(scaladocClassName));
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
                getLog().warn("No source files found in " + sourceDir);
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

            if (sourceDir.exists()) {
                JavaMainCaller jcmd = getScalaCommand();
                jcmd.addOption("-d", reportOutputDir.getAbsolutePath());
                String[] sources = findSourceFiles();
                if (sources.length > 0) {
                    for (String x : sources) {
                        jcmd.addArgs(sourceDir + File.separator + x);
                    }
                    jcmd.run(displayCmd);
                }
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

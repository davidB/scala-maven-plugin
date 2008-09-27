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

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;

import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.plexus.util.StringUtils;

/**
 * Produces Scala API documentation.
 *
 * @goal doc
 * @requiresDependencyResolution compile
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
     * @parameter expression="${linksource}" default="true"
     */
    protected boolean linksource;

    /**
     * Suppress description and tags, generate only declarations
     *
     * @parameter expression="${nocomment}" default="false"
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


    private String[] sourceFiles_ = null;
    private String[] findSourceFiles() {
        if (sourceFiles_ == null) {
            sourceFiles_ = JavaCommand.findFiles(sourceDir, "**/*.scala");
        }
        return sourceFiles_;
    }

    public boolean canGenerateReport() {
        try {
            sourceDir = sourceDir.getCanonicalFile();
        } catch (IOException exc) {
            sourceDir = sourceDir.getAbsoluteFile();
        }
        return sourceDir.exists() && (findSourceFiles().length != 0);
    }

    public boolean isExternalReport() {
        return true;
    }

    public String getCategoryName() {
        return CATEGORY_PROJECT_REPORTS;
    }

    public String getDescription(Locale locale) {
        if (StringUtils.isEmpty(description)) {
            return "ScalaDoc API documentation";
        }
        return description;
    }

    public String getName(Locale locale) {
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
            reportOutputDirectory = new File(project.getReporting().getOutputDirectory(), outputDirectory);
        }
        return reportOutputDirectory;
    }

    public void setReportOutputDirectory(File reportOutputDirectory) {
        this.reportOutputDirectory = new File(reportOutputDirectory, outputDirectory);
    }

    @Override
    public void doExecute() throws Exception {
        // SiteRendererSink sink = siteRenderer.createSink(new
        // File(project.getReporting().getOutputDirectory(), getOutputName() +
        // ".html");
        generate(null, Locale.getDefault());
    }


    @Override
    protected JavaCommand getScalaCommand() throws Exception {
        String oldClazz = scalaClassName;
        boolean isPreviousScala271 = (new VersionNumber("2.7.1").compareTo(new VersionNumber(scalaVersion)) > 0);
        if (!isPreviousScala271) {
            scalaClassName = "scala.tools.nsc.ScalaDoc";
        }
        if (StringUtils.isNotEmpty(scaladocClassName)) {
            scalaClassName = scaladocClassName;
        }
        JavaCommand cmd = getEmptyScalaCommand(scalaClassName);
        cmd.addArgs(args);
        cmd.addJvmArgs(jvmArgs);
        if (isPreviousScala271){
            cmd.addArgs("-Ydoc");
        }
        scalaClassName = oldClazz;
        return cmd;
    }

    @SuppressWarnings("unchecked")
    public void generate(Sink sink, Locale locale) throws MavenReportException {
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
            JavaCommand jcmd = getScalaCommand();
            jcmd.addOption("-classpath", JavaCommand.toMultiPath(project.getCompileClasspathElements()));
            jcmd.addOption("-d", reportOutputDir.getAbsolutePath());
            jcmd.addOption("-sourcepath", sourceDir.getAbsolutePath());
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
            for (String x : findSourceFiles()) {
                jcmd.addArgs(sourceDir + File.separator + x);
            }
            jcmd.run(displayCmd);
        } catch (MavenReportException exc) {
            throw exc;
        } catch (RuntimeException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new MavenReportException("wrap: " + exc.getMessage(), exc);
        }
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

package scala_maven;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.plexus.util.StringUtils;

import scala_maven_executions.JavaMainCaller;
import scala_maven_executions.MainHelper;

/**
 * Produces Scala API documentation.
 *
 */
@Mojo(name = "doc", requiresDependencyResolution = ResolutionScope.COMPILE)
@Execute(phase = LifecyclePhase.GENERATE_RESOURCES)
public class ScalaDocMojo extends ScalaSourceMojoSupport implements MavenReport {

    /**
     * Specify window title of generated HTML documentation.
     * [scaladoc, vscaladoc]
     *
     */
    @Parameter(property = "windowtitle", defaultValue = "${project.name} ${project.version} API")
    protected String windowtitle;

    /**
     * Specifies the text to be placed at the bottom of each output file. If you
     * want to use html you have to put it in a CDATA section, eg.
     * &lt;![CDATA[Copyright 2005, &lt;a
     * href="http://www.mycompany.com">MyCompany, Inc.&lt;a>]]&gt;
     * [scaladoc, vscaladoc]
     *
     */
    @Parameter(property = "bottom", defaultValue = "Copyright (c) {inceptionYear}-{currentYear} {organizationName}. All Rights Reserved.")
    protected String bottom;

    /**
     * Charset for cross-platform viewing of generated documentation.
     * [scaladoc, vscaladoc]
     *
     */
    @Parameter(property = "charset", defaultValue = "ISO-8859-1")
    protected String charset;

    /**
     * Include title for the overview page.
     * [scaladoc, scaladoc2, vscaladoc]
     *
     */
    @Parameter(property = "doctitle", defaultValue = "${project.name} ${project.version} API")
    protected String doctitle;

    /**
     * Include footer text for each page.
     * [scaladoc, vscaladoc]
     *
     */
    @Parameter(property = "footer")
    protected String footer;

    /**
     * Include header text for each page
     * [scaladoc, vscaladoc]
     *
     */
    @Parameter(property = "header")
    protected String header;

    /**
     * Generate source in HTML
     * [scaladoc, vscaladoc]
     *
     */
    @Parameter(property = "linksource", defaultValue = "true")
    protected boolean linksource;

    /**
     * Suppress description and tags, generate only declarations
     * [scaladoc, vscaladoc]
     *
     */
    @Parameter(property = "nocomment", defaultValue = "false")
    protected boolean nocomment;

    /**
     * File to change style of the generated documentation
     * [scaladoc, vscaladoc]
     *
     */
    @Parameter(property = "stylesheetfile")
    protected File stylesheetfile;

    /**
     * Include top text for each page
     * [scaladoc, vscaladoc]
     *
     */
    @Parameter(property = "top")
    protected String top;

    /**
     * Specifies the destination directory where scalaDoc saves the generated
     * HTML files.
     *
     */
    @Parameter(defaultValue = "scaladocs", required = true)
    protected String outputDirectory;

    /**
     * Specifies the destination directory where javadoc saves the generated HTML
     * files.
     *
     */
    @Parameter(defaultValue = "${project.reporting.outputDirectory}/scaladocs", required = true)
    protected File reportOutputDirectory;

    /**
     * The name of the Scaladoc report.
     *
     * @since 2.1
     */
    @Parameter(property = "name", defaultValue = "ScalaDocs")
    private String name;

    /**
     * The description of the Scaladoc report.
     *
     * @since 2.1
     */
    @Parameter(property = "description", defaultValue = "ScalaDoc API documentation.")
    private String description;

    /**
     * className (FQN) of the main scaladoc to use, if not define, the the
     * scalaClassName is used
     *
     */
    @Parameter(property = "maven.scaladoc.className")
    protected String scaladocClassName;

    /**
     * If you want to use vscaladoc to generate api instead of regular scaladoc, set
     * the version of vscaladoc you want to use.
     *
     */
    @Parameter(property = "maven.scaladoc.vscaladocVersion")
    protected String vscaladocVersion;

    /**
     * To allow running aggregation only from command line use
     * "-DforceAggregate=true" (avoid using in pom.xml).
     * [scaladoc, vscaladoc]
     *
     */
    @Parameter(property = "forceAggregate", defaultValue = "false")
    protected boolean forceAggregate = false;

    /**
     * If you want to aggregate only direct sub modules.
     *
     */
    @Parameter(property = "maven.scaladoc.aggregateDirectOnly", defaultValue = "true")
    protected boolean aggregateDirectOnly = true;

    /**
     * The directory which contains scala/java source files
     *
     */
    @Parameter(defaultValue = "${project.build.sourceDirectory}/../scala")
    protected File sourceDir;

    private List<File> _sourceFiles;

    @Override
    protected List<File> getSourceDirectories() throws Exception {
        List<String> sources = project.getCompileSourceRoots();
        //Quick fix in case the user has not added the "add-source" goal.
        String scalaSourceDir = FileUtils.pathOf(sourceDir, useCanonicalPath);
        if(!sources.contains(scalaSourceDir)) {
            sources.add(scalaSourceDir);
        }
        return normalize(sources);
    }

    @Override
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

    @Override
    public boolean isExternalReport() {
        return true;
    }

    @Override
    public String getCategoryName() {
        return CATEGORY_PROJECT_REPORTS;
    }

    @Override
    public String getDescription(Locale locale) {
        if (StringUtils.isEmpty(description)) {
            return "ScalaDoc API documentation";
        }
        return description;
    }

    @Override
    public String getName(Locale locale) {
        if (StringUtils.isEmpty(name)) {
            return "ScalaDocs";
        }
        return name;
    }

    @Override
    public String getOutputName() {
        return outputDirectory + "/index";
    }

    @Override
    public File getReportOutputDirectory() {
        if (reportOutputDirectory == null) {
            reportOutputDirectory = new File(project.getBasedir(), project.getReporting().getOutputDirectory() + "/" + outputDirectory).getAbsoluteFile();
        }
        return reportOutputDirectory;
    }

    @Override
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


    @Override
    protected JavaMainCaller getScalaCommand() throws Exception {
        //This ensures we have a valid scala version...
        checkScalaVersion();
        VersionNumber sv = findScalaVersion();
        boolean isPreviousScala271 = (new VersionNumber("2.7.1").compareTo(sv) > 0 && !sv.isZero());
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
        addCompilerPluginOptions(jcmd);

        if (isPreviousScala271){
            jcmd.addArgs("-Ydoc");
        }
        // copy the classpathElements to not modify the global project definition see https://github.com/davidB/maven-scala-plugin/issues/60
        List<String> paths = new ArrayList<String>(project.getCompileClasspathElements());
        paths.remove(project.getBuild().getOutputDirectory()); //remove output to avoid "error for" : error:  XXX is already defined as package XXX ... object XXX {
        if (!paths.isEmpty())jcmd.addOption("-classpath", MainHelper.toMultiPath(paths));
        //jcmd.addOption("-sourcepath", sourceDir.getAbsolutePath());

        boolean isScaladoc2 = (new VersionNumber("2.8.0").compareTo(sv) <= 0 || sv.isZero()) && ("scala.tools.nsc.ScalaDoc".equals(scaladocClassName));
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

    @Override
    public void generate(Sink sink, Locale locale) throws MavenReportException {
        try {
            if (!canGenerateReport()) {
                getLog().info("No source files found");
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
                    jcmd.addArgs(FileUtils.pathOf(x, useCanonicalPath));
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

    protected void tryAggregateUpper(MavenProject prj) throws Exception {
        if (prj != null && prj.hasParent() && canAggregate()) {
            MavenProject parent = prj.getParent();
            List<MavenProject> modules = parent.getCollectedProjects();
            if ((modules.size() > 1) && prj.equals(modules.get(modules.size() - 1))) {
                aggregate(parent);
            }
        }
    }

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

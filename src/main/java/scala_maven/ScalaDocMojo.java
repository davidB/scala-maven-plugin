/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package scala_maven;

import java.io.File;
import java.util.*;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.reporting.MavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.plexus.util.StringUtils;
import scala_maven_dependency.Context;
import scala_maven_executions.JavaMainCaller;
import util.FileUtils;

/** Produces Scala API documentation. */
@Mojo(name = "doc", requiresDependencyResolution = ResolutionScope.COMPILE)
@Execute(phase = LifecyclePhase.GENERATE_SOURCES)
public class ScalaDocMojo extends ScalaSourceMojoSupport implements MavenReport {

  /** Include title for the overview page. */
  @Parameter(property = "doctitle", defaultValue = "${project.name} ${project.version} API")
  private String doctitle;

  /** Specifies the destination directory where scalaDoc saves the generated HTML files. */
  @Parameter(defaultValue = "scaladocs", required = true)
  private String outputDirectory;

  /** Specifies the destination directory where javadoc saves the generated HTML files. */
  @Parameter(defaultValue = "${project.reporting.outputDirectory}/scaladocs", required = true)
  File reportOutputDirectory;

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

  /** className (FQN) of the main scaladoc to use, if not define, the the scalaClassName is used */
  @Parameter(property = "maven.scaladoc.className")
  private String scaladocClassName;

  /** The directory which contains scala/java source files */
  @Parameter(defaultValue = "${project.build.sourceDirectory}/../scala")
  private File sourceDir;

  private List<File> _sourceFiles;

  @Override
  protected List<File> getSourceDirectories() throws Exception {
    List<String> sources = project.getCompileSourceRoots();
    // Quick fix in case the user has not added the "add-source" goal.
    String scalaSourceDir = FileUtils.pathOf(sourceDir, useCanonicalPath);
    if (!sources.contains(scalaSourceDir)) {
      sources.add(scalaSourceDir);
    }
    return normalize(sources);
  }

  @Override
  public boolean canGenerateReport() {
    return findSourceFiles().size() != 0;
  }

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
      reportOutputDirectory =
          new File(
                  project.getBasedir(),
                  project.getModel().getReporting().getOutputDirectory() + "/" + outputDirectory)
              .getAbsoluteFile();
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

  protected JavaMainCaller getScalaCommand() throws Exception {
    // This ensures we have a valid scala version...
    checkScalaVersion();
    Context sc = findScalaContext();
    JavaMainCaller jcmd = getEmptyScalaCommand(sc.apidocMainClassName(scaladocClassName));
    jcmd.addArgs(args);
    jcmd.addJvmArgs(jvmArgs);
    addCompilerPluginOptions(jcmd);

    // copy the classpathElements to not modify the global project definition see
    // https://github.com/davidB/scala-maven-plugin/issues/60
    Set<File> paths = new TreeSet<>();
    for (String s : project.getCompileClasspathElements()) {
      paths.add(new File(s));
    }
    paths.remove(
        new File(
            project
                .getBuild()
                .getOutputDirectory())); // remove output to avoid "error for" : error: XXX is
    // already defined as package XXX ... object XXX {
    addAdditionalDependencies(paths);
    if (!paths.isEmpty()) jcmd.addOption("-classpath", FileUtils.toMultiPath(paths));
    // jcmd.addOption("-sourcepath", sourceDir.getAbsolutePath());

    jcmd.addArgs("-doc-format:html");
    jcmd.addOption("-doc-title", doctitle);
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

      List<File> sources = findSourceFiles();
      if (sources.size() > 0) {
        JavaMainCaller jcmd = getScalaCommand();
        jcmd.addOption("-d", reportOutputDir.getAbsolutePath());
        for (File x : sources) {
          jcmd.addArgs(FileUtils.pathOf(x, useCanonicalPath));
        }
        jcmd.run(displayCmd);
      }
    } catch (MavenReportException | RuntimeException exc) {
      throw exc;
    } catch (Exception exc) {
      throw new MavenReportException("wrap: " + exc.getMessage(), exc);
    }
  }
}
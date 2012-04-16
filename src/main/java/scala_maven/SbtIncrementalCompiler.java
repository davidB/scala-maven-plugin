package scala_maven;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URLClassLoader;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import sbt.incremental.IncrementalCompiler;
import sbt.Logger;
import scala.collection.JavaConverters;
import scala.collection.Seq;
import scala.collection.Seq$;

public class SbtIncrementalCompiler {

    private Log log;

    private xsbti.Logger logger;

    private IncrementalCompiler compiler;

    public SbtIncrementalCompiler(String version, File libraryJar, File compilerJar, String sbtVersion, File xsbtiJar, File interfaceJar, Log log) throws Exception {
        log.info("Using incremental compilation");
        this.log = log;
        this.logger = new SbtLogger(log);
        this.compiler = new IncrementalCompiler(version, libraryJar, compilerJar, sbtVersion, xsbtiJar, interfaceJar, logger);
    }

    public void compile(List<String> classpathElements, List<File> sourcesList, File outputDir, List<String> scalacOptions, List<String> javacOptions) {
        Seq<File> classpath = listToSeq(pathsToFiles(classpathElements));
        Seq<File> sources = listToSeq(sourcesList);
        Seq<String> soptions = listToSeq(scalacOptions);
        Seq<String> joptions = listToSeq(javacOptions);
        compiler.compile(classpath, sources, outputDir, soptions, joptions);
    }

    public <A> Seq<A> listToSeq(List<A> list) {
        return JavaConverters.collectionAsScalaIterableConverter(list).asScala().toSeq();
    }

    public List<File> pathsToFiles(List<String> paths) {
        List<File> files = new ArrayList<File>(paths.size());
        for (String path : paths) {
            files.add(new File(path));
        }
        return files;
    }
}

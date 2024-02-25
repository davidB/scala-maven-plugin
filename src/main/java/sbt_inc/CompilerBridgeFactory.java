/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package sbt_inc;

import static scala.jdk.CollectionConverters.IterableHasAsScala;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import sbt.internal.inc.RawCompiler;
import sbt.internal.inc.ScalaInstance;
import sbt.io.AllPassFilter$;
import sbt.io.IO;
import scala.Tuple2;
import scala_maven.MavenArtifactResolver;
import scala_maven.VersionNumber;
import util.FileUtils;
import xsbti.compile.ClasspathOptionsUtil;

public final class CompilerBridgeFactory {

  private static final String SBT_GROUP_ID = "org.scala-sbt";
  private static final String SBT_GROUP_ID_SCALA3 = "org.scala-lang";
  private static final String JAVA_CLASS_VERSION = System.getProperty("java.class.version");

  private static final File DEFAULT_SECONDARY_CACHE_DIR =
      Paths.get(System.getProperty("user.home"), ".sbt", "1.0", "zinc", "org.scala-sbt").toFile();

  private CompilerBridgeFactory() {}

  static File getCompiledBridgeJar(
      VersionNumber scalaVersion,
      ScalaInstance scalaInstance,
      File secondaryCacheDir,
      MavenArtifactResolver resolver,
      Log mavenLogger)
      throws Exception {
    // eg
    // org.scala-sbt-compiler-bridge_2.12-1.2.4-bin_2.12.10__52.0-1.2.4_20181015T090407.jar
    String bridgeArtifactId = compilerBridgeArtifactId(scalaVersion.toString());

    if (secondaryCacheDir == null) {
      secondaryCacheDir = DEFAULT_SECONDARY_CACHE_DIR;
    }
    secondaryCacheDir.mkdirs();

    return scalaVersion.major == 3
        ? getScala3CompilerBridgeJar(scalaVersion, bridgeArtifactId, resolver)
        : getScala2CompilerBridgeJar(
            scalaInstance,
            scalaVersion,
            bridgeArtifactId,
            resolver,
            secondaryCacheDir,
            mavenLogger);
  }

  private static String compilerBridgeArtifactId(String scalaVersion) {
    if (scalaVersion.startsWith("2.10.")) {
      return "compiler-bridge_2.10";
    } else if (scalaVersion.startsWith("2.11.")) {
      return "compiler-bridge_2.11";
    } else if (scalaVersion.startsWith("2.12.") || scalaVersion.equals("2.13.0-M1")) {
      return "compiler-bridge_2.12";
    } else if (scalaVersion.startsWith("2.13.")) {
      return "compiler-bridge_2.13";
    } else {
      return "scala3-sbt-bridge";
    }
  }

  private static File getScala3CompilerBridgeJar(
      VersionNumber scalaVersion, String bridgeArtifactId, MavenArtifactResolver resolver) {
    return resolver
        .getJar(SBT_GROUP_ID_SCALA3, bridgeArtifactId, scalaVersion.toString(), "")
        .getFile();
  }

  private static File getScala2CompilerBridgeJar(
      ScalaInstance scalaInstance,
      VersionNumber scalaVersion,
      String bridgeArtifactId,
      MavenArtifactResolver resolver,
      File secondaryCacheDir,
      Log mavenLogger)
      throws IOException {
    // this file is localed in compiler-interface
    Properties properties = new Properties();
    try (InputStream is =
        CompilerBridgeFactory.class
            .getClassLoader()
            .getResourceAsStream("incrementalcompiler.version.properties")) {
      properties.load(is);
    }

    String zincVersion = properties.getProperty("version");
    String timestamp = properties.getProperty("timestamp");

    String cacheFileName =
        SBT_GROUP_ID
            + '-'
            + bridgeArtifactId
            + '-'
            + zincVersion
            + "-bin_"
            + scalaVersion
            + "__"
            + JAVA_CLASS_VERSION
            + '-'
            + zincVersion
            + '_'
            + timestamp
            + ".jar";

    File cachedCompiledBridgeJar = new File(secondaryCacheDir, cacheFileName);

    if (mavenLogger.isInfoEnabled()) {
      mavenLogger.info("Compiler bridge file: " + cachedCompiledBridgeJar);
    }

    if (!cachedCompiledBridgeJar.exists()) {
      mavenLogger.info("Compiler bridge file is not installed yet");
      // compile and install
      RawCompiler rawCompiler =
          new RawCompiler(
              scalaInstance, ClasspathOptionsUtil.auto(), new MavenLoggerSbtAdapter(mavenLogger));

      File bridgeSources =
          resolver.getJar(SBT_GROUP_ID, bridgeArtifactId, zincVersion, "sources").getFile();

      Set<Path> bridgeSourcesDependencies =
          resolver
              .getJarAndDependencies(SBT_GROUP_ID, bridgeArtifactId, zincVersion, "sources")
              .stream()
              .filter(
                  artifact ->
                      artifact.getScope() != null && !artifact.getScope().equals("provided"))
              .map(Artifact::getFile)
              .map(File::toPath)
              .collect(Collectors.toSet());

      bridgeSourcesDependencies.addAll(
          Arrays.stream(scalaInstance.allJars())
              .sequential()
              .map(File::toPath)
              .collect(Collectors.toList()));

      Path sourcesDir = Files.createTempDirectory("scala-maven-plugin-compiler-bridge-sources");
      Path classesDir = Files.createTempDirectory("scala-maven-plugin-compiler-bridge-classes");

      IO.unzip(bridgeSources, sourcesDir.toFile(), AllPassFilter$.MODULE$, true);

      List<Path> bridgeSourcesScalaFiles =
          FileUtils.listDirectoryContent(
              sourcesDir,
              file ->
                  Files.isRegularFile(file) && file.getFileName().toString().endsWith(".scala"));
      List<Path> bridgeSourcesNonScalaFiles =
          FileUtils.listDirectoryContent(
              sourcesDir,
              file ->
                  Files.isRegularFile(file)
                      && !file.getFileName().toString().endsWith(".scala")
                      && !file.getFileName().toString().equals("MANIFEST.MF"));

      try {
        rawCompiler.apply(
            IterableHasAsScala(bridgeSourcesScalaFiles).asScala().toSeq(), // sources:Seq[File]
            IterableHasAsScala(bridgeSourcesDependencies).asScala().toSeq(), // classpath:Seq[File],
            classesDir, // outputDirectory:Path,
            IterableHasAsScala(Collections.<String>emptyList())
                .asScala()
                .toSeq() // options:Seq[String]
            );

        Manifest manifest = new Manifest();
        Path sourcesManifestFile = sourcesDir.resolve("META-INF").resolve("MANIFEST.MF");
        try (InputStream is = Files.newInputStream(sourcesManifestFile)) {
          manifest.read(is);
        }

        List<Tuple2<File, String>> scalaCompiledClasses =
            computeZipEntries(FileUtils.listDirectoryContent(classesDir, file -> true), classesDir);
        List<Tuple2<File, String>> resources =
            computeZipEntries(bridgeSourcesNonScalaFiles, sourcesDir);
        List<Tuple2<File, String>> allZipEntries = new ArrayList<>();
        allZipEntries.addAll(scalaCompiledClasses);
        allZipEntries.addAll(resources);

        IO.jar(
            IterableHasAsScala(
                    allZipEntries.stream()
                        .map(x -> scala.Tuple2.apply(x._1, x._2))
                        .collect(Collectors.toList()))
                .asScala(),
            cachedCompiledBridgeJar,
            manifest);

        mavenLogger.info("Compiler bridge installed");

      } finally {
        FileUtils.deleteDirectory(sourcesDir);
        FileUtils.deleteDirectory(classesDir);
      }
    }

    return cachedCompiledBridgeJar;
  }

  private static List<Tuple2<File, String>> computeZipEntries(List<Path> paths, Path rootDir) {
    int rootDirLength = rootDir.toString().length();
    Stream<Tuple2<File, String>> stream =
        paths.stream()
            .map(
                path -> {
                  String zipPath =
                      path.toString().substring(rootDirLength + 1).replace(File.separator, "/");
                  if (Files.isDirectory(path)) {
                    zipPath = zipPath + "/";
                  }
                  return new Tuple2<>(path.toFile(), zipPath);
                });
    return stream.collect(Collectors.toList());
  }
}

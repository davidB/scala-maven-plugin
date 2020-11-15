
/*
 * Copyright 2011-2020 scala-maven-plugin project (https://davidb.github.io/scala-maven-plugin/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sbt_inc;

import static scala.jdk.CollectionConverters.IterableHasAsScala;
import static scala.jdk.FunctionWrappers.FromJavaConsumer;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import sbt.internal.inc.*;
import sbt.internal.inc.FileAnalysisStore;
import sbt.internal.inc.ScalaInstance;
import sbt.internal.inc.classpath.ClasspathUtil;
import sbt.io.AllPassFilter$;
import sbt.io.IO;
import sbt.util.Logger;
import scala.Option;
import scala.Tuple2;
import scala_maven.MavenArtifactResolver;
import scala_maven.VersionNumber;
import util.FileUtils;
import xsbti.PathBasedFile;
import xsbti.T2;
import xsbti.VirtualFile;
import xsbti.compile.*;

public class SbtIncrementalCompiler {

  private static final String SBT_GROUP_ID = "org.scala-sbt";
  private static final String JAVA_CLASS_VERSION = System.getProperty("java.class.version");
  private static final File DEFAULT_SECONDARY_CACHE_DIR =
      Paths.get(System.getProperty("user.home"), ".sbt", "1.0", "zinc", "org.scala-sbt").toFile();

  private final IncrementalCompiler compiler = ZincUtil.defaultIncrementalCompiler();
  private final CompileOrder compileOrder;
  private final Logger logger;
  private final Compilers compilers;
  private final Setup setup;
  private final AnalysisStore analysisStore;
  private final MavenArtifactResolver resolver;
  private final File secondaryCacheDir;

  public SbtIncrementalCompiler(
      File libraryJar,
      File reflectJar,
      File compilerJar,
      VersionNumber scalaVersion,
      List<File> extraJars,
      Path javaHome,
      MavenArtifactResolver resolver,
      File secondaryCacheDir,
      Log mavenLogger,
      File cacheFile,
      CompileOrder compileOrder)
      throws Exception {
    this.compileOrder = compileOrder;
    this.logger = new SbtLogger(mavenLogger);
    mavenLogger.info("Using incremental compilation using " + compileOrder + " compile order");
    this.resolver = resolver;
    this.secondaryCacheDir =
        secondaryCacheDir != null ? secondaryCacheDir : DEFAULT_SECONDARY_CACHE_DIR;
    this.secondaryCacheDir.mkdirs();

    List<File> allJars = new ArrayList<>(extraJars);
    allJars.add(libraryJar);
    allJars.add(reflectJar);
    allJars.add(compilerJar);

    ScalaInstance scalaInstance =
        new ScalaInstance(
            scalaVersion.toString(), // version
            new URLClassLoader(
                new URL[] {
                  libraryJar.toURI().toURL(),
                  reflectJar.toURI().toURL(),
                  compilerJar.toURI().toURL()
                }), // loader
            ClasspathUtil.rootLoader(), // loaderLibraryOnly
            libraryJar, // libraryJar
            compilerJar, // compilerJar
            allJars.toArray(new File[] {}), // allJars
            Option.apply(scalaVersion.toString()) // explicitActual
            );

    File compilerBridgeJar = getCompiledBridgeJar(scalaInstance, mavenLogger);

    ScalaCompiler scalaCompiler =
        new AnalyzingCompiler(
            scalaInstance, // scalaInstance
            ZincCompilerUtil.constantBridgeProvider(scalaInstance, compilerBridgeJar), // provider
            ClasspathOptionsUtil.auto(), // classpathOptions
            new FromJavaConsumer(noop -> {}), // onArgsHandler
            Option.apply(null) // classLoaderCache
            );

    compilers =
        ZincUtil.compilers(
            scalaInstance, ClasspathOptionsUtil.boot(), Option.apply(javaHome), scalaCompiler);

    PerClasspathEntryLookup lookup =
        new PerClasspathEntryLookup() {
          @Override
          public Optional<CompileAnalysis> analysis(VirtualFile classpathEntry) {
            Path path = ((PathBasedFile) classpathEntry).toPath();

            String analysisStoreFileName = null;
            if (Files.isDirectory(path)) {
              if (path.getFileName().equals("classes")) {
                analysisStoreFileName = "compile";

              } else if (path.getFileName().equals("test-classes")) {
                analysisStoreFileName = "test-compile";
              }
            }

            if (analysisStoreFileName != null) {
              File analysisStoreFile =
                  path.getParent().resolve("analysis").resolve(analysisStoreFileName).toFile();
              if (analysisStoreFile.exists()) {
                return AnalysisStore.getCachedStore(FileAnalysisStore.binary(analysisStoreFile))
                    .get()
                    .map(AnalysisContents::getAnalysis);
              }
            }
            return Optional.empty();
          }

          @Override
          public DefinesClass definesClass(VirtualFile classpathEntry) {
            return Locate.definesClass(classpathEntry);
          }
        };

    analysisStore = AnalysisStore.getCachedStore(FileAnalysisStore.binary(cacheFile));

    setup =
        Setup.of(
            lookup, // lookup
            false, // skip
            cacheFile, // cacheFile
            CompilerCache.fresh(), // cache
            IncOptions.of(), // incOptions
            new LoggedReporter(100, logger, pos -> pos), // reporter
            Optional.empty(), // optionProgress
            new T2[] {});
  }

  private PreviousResult previousResult() {
    Optional<AnalysisContents> analysisContents = analysisStore.get();
    if (analysisContents.isPresent()) {
      AnalysisContents analysisContents0 = analysisContents.get();
      CompileAnalysis previousAnalysis = analysisContents0.getAnalysis();
      MiniSetup previousSetup = analysisContents0.getMiniSetup();
      return PreviousResult.of(Optional.of(previousAnalysis), Optional.of(previousSetup));
    } else {
      return PreviousResult.of(Optional.empty(), Optional.empty());
    }
  }

  public void compile(
      Set<String> classpathElements,
      List<Path> sources,
      Path classesDirectory,
      List<String> scalacOptions,
      List<String> javacOptions) {
    List<Path> fullClasspath = new ArrayList<>();
    fullClasspath.add(classesDirectory);
    for (String classpathElement : classpathElements) {
      fullClasspath.add(Paths.get(classpathElement));
    }

    CompileOptions options =
        CompileOptions.of(
            fullClasspath.stream()
                .map(PlainVirtualFile::new)
                .toArray(VirtualFile[]::new), // classpath
            sources.stream().map(PlainVirtualFile::new).toArray(VirtualFile[]::new), // sources
            classesDirectory, //
            scalacOptions.toArray(new String[] {}), // scalacOptions
            javacOptions.toArray(new String[] {}), // javacOptions
            100, // maxErrors
            pos -> pos, // sourcePositionMappers
            compileOrder, // order
            Optional.empty(), // temporaryClassesDirectory
            Optional.of(PlainVirtualFileConverter.converter()), // _converter
            Optional.empty(), // _stamper
            Optional.empty() // _earlyOutput
            );

    Inputs inputs = Inputs.of(compilers, options, setup, previousResult());

    CompileResult newResult = compiler.compile(inputs, logger);
    analysisStore.set(AnalysisContents.create(newResult.analysis(), newResult.setup()));
  }

  private String compilerBridgeArtifactId(String scalaVersion) {
    if (scalaVersion.startsWith("2.10.")) {
      return "compiler-bridge_2.10";
    } else if (scalaVersion.startsWith("2.11.")) {
      return "compiler-bridge_2.11";
    } else if (scalaVersion.startsWith("2.12.") || scalaVersion.equals("2.13.0-M1")) {
      return "compiler-bridge_2.12";
    } else {
      return "compiler-bridge_2.13";
    }
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
                  return new Tuple2(path.toFile(), zipPath);
                });
    return stream.collect(Collectors.toList());
  }

  private File getCompiledBridgeJar(ScalaInstance scalaInstance, Log mavenLogger) throws Exception {

    // eg
    // org.scala-sbt-compiler-bridge_2.12-1.2.4-bin_2.12.10__52.0-1.2.4_20181015T090407.jar
    String bridgeArtifactId = compilerBridgeArtifactId(scalaInstance.actualVersion());

    // this file is localed in compiler-interface
    Properties properties = new Properties();
    try (InputStream is =
        getClass().getClassLoader().getResourceAsStream("incrementalcompiler.version.properties")) {
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
            + scalaInstance.actualVersion()
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
      RawCompiler rawCompiler = new RawCompiler(scalaInstance, ClasspathOptionsUtil.auto(), logger);

      File bridgeSources =
          resolver.getJar(SBT_GROUP_ID, bridgeArtifactId, zincVersion, "sources").getFile();

      Set<Path> bridgeSourcesDependencies =
          resolver.getJarAndDependencies(SBT_GROUP_ID, bridgeArtifactId, zincVersion, "sources")
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
        try (InputStream is = new FileInputStream(sourcesManifestFile.toFile())) {
          manifest.read(is);
        }

        List<Tuple2<File, String>> scalaCompiledClasses =
            computeZipEntries(FileUtils.listDirectoryContent(classesDir, file -> true), classesDir);
        List<Tuple2<File, String>> resources =
            computeZipEntries(bridgeSourcesNonScalaFiles, sourcesDir);
        List<Tuple2<File, String>> allZipEntries = new ArrayList<>();
        allZipEntries.addAll(scalaCompiledClasses);
        allZipEntries.addAll(resources);

        IO.jar(IterableHasAsScala(allZipEntries).asScala(), cachedCompiledBridgeJar, manifest);

        mavenLogger.info("Compiler bridge installed");

      } finally {
        FileUtils.deleteDirectory(sourcesDir);
        FileUtils.deleteDirectory(classesDir);
      }
    }

    return cachedCompiledBridgeJar;
  }
}

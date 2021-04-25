
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
package scala_maven_dependency;

import java.io.File;
import java.util.Set;
import org.apache.maven.artifact.Artifact;
import scala_maven.VersionNumber;

public class Context4ScalaHome implements Context {
  private File scalaHome;
  private final VersionNumber scalaVersion;
  private final VersionNumber scalaCompatVersion;
  private ArtifactIds aids;

  public Context4ScalaHome(
      VersionNumber scalaVersion,
      VersionNumber scalaCompatVersion,
      ArtifactIds aids,
      File scalaHome) {
    this.scalaHome = scalaHome;
    this.scalaVersion = scalaVersion;
    this.scalaCompatVersion = scalaCompatVersion;
    this.aids = aids;
  }

  @Override
  public boolean hasInDistro(Artifact artifact) throws Exception {
    return false;
  }

  @Override
  public VersionNumber version() {
    return scalaVersion;
  }

  @Override
  public VersionNumber versionCompat() {
    return scalaCompatVersion;
  }

  @Override
  public File findLibraryJar() throws Exception {
    File lib = new File(scalaHome, "lib");
    return new File(lib, aids.scalaLibraryArtifactId() + ".jar");
  }

  @Override
  public File findReflectJar() throws Exception {
    File lib = new File(scalaHome, "lib");
    return new File(lib, aids.scalaReflectArtifactId() + ".jar");
  }

  @Override
  public File findCompilerJar() throws Exception {
    File lib = new File(scalaHome, "lib");
    return new File(lib, aids.scalaCompilerArtifactId() + ".jar");
  }

  @Override
  public Set<Artifact> findCompilerAndDependencies() throws Exception {
    throw new UnsupportedOperationException("TODO local scalaHome do not provide valid artifact");
    //    String compiler = aids.scalaCompilerArtifactId();
    //    String library = aids.scalaLibraryArtifactId();
    //    List<File> d = new ArrayList<>();
    //    for (File f : new File(scalaHome, "lib").listFiles()) {
    //      String name = f.getName();
    //      if (name.endsWith(".jar")
    //          // && !name.contains(compiler)
    //          && !name.contains(library)) {
    //        d.add(f);
    //      }
    //    }
    //    return d;
  }
}

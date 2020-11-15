
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
package scala_maven;

import static org.junit.Assert.*;

import java.io.File;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.strategy.SelfFirstStrategy;
import org.codehaus.plexus.classworlds.strategy.Strategy;
import org.codehaus.plexus.util.StringUtils;
import org.junit.Test;

/** @author david bernard */
public class MiscTest {

  // TODO not use String.split, it is not consistent across jdk version
  // see https://github.com/davidB/scala-maven-plugin/pull/206
  // public void testJdkSplit() throws Exception {
  // assertEquals("Are you using JDK > 7? This failure is expected above JDK 7.",
  // 6, "hello".split("|").length);
  // assertEquals(1, "hello".split("\\|").length);
  // assertEquals(2, "hel|lo".split("\\|").length);
  // assertEquals(3, "hel||lo".split("\\|").length);
  // }

  @Test
  public void stringUtilsSplit() throws Exception {
    assertEquals(1, StringUtils.split("hello", "|").length);
    assertEquals(1, StringUtils.split("hello|", "|").length);
    assertEquals(2, StringUtils.split("hel|lo", "|").length);
    assertEquals(2, StringUtils.split("hel||lo", "|").length);
  }

  @Test
  public void classworldSeftFirstStrategy() throws Exception {
    ClassWorld w = new ClassWorld("zero", null);
    ClassRealm rMojo = w.newRealm("mojo", getClass().getClassLoader());
    Strategy s = new SelfFirstStrategy(w.newRealm("scalaScript", null));
    ClassRealm rScript = s.getRealm();
    rScript.setParentClassLoader(getClass().getClassLoader());
    rScript.importFrom("mojo", MavenProject.class.getPackage().getName());
    rScript.importFrom("mojo", MavenSession.class.getPackage().getName());
    rScript.importFrom("mojo", Log.class.getPackage().getName());

    assertEquals(rScript, rScript.getStrategy().getRealm());
    assertEquals(SelfFirstStrategy.class, rScript.getStrategy().getClass());

    File olderjar =
        new File(
            System.getProperty("user.home"),
            ".m2/repository/net/alchim31/maven/scala-maven-plugin/3.1.0/scala-maven-plugin-3.1.0.jar");
    if (olderjar.exists()) {
      System.out.println("found older jar");
      rScript.addURL(olderjar.toURI().toURL());
      String clname = "scala_maven.ScalaScriptMojo";
      // assertNotSame(s.loadClass(clname),
      // getClass().getClassLoader().loadClass(clname));
      assertNotSame(rScript.loadClass(clname), getClass().getClassLoader().loadClass(clname));
      assertSame(rMojo.loadClass(clname), getClass().getClassLoader().loadClass(clname));
      assertSame(rScript.loadClass(MavenProject.class.getCanonicalName()), MavenProject.class);
      assertSame(rScript.loadClass(MavenSession.class.getCanonicalName()), MavenSession.class);
      assertSame(rScript.loadClass(Log.class.getCanonicalName()), Log.class);
    }
  }
}

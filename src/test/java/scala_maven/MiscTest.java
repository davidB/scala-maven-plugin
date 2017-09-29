/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scala_maven;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.strategy.SelfFirstStrategy;
import org.codehaus.plexus.classworlds.strategy.Strategy;
import org.codehaus.plexus.util.StringUtils;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 *
 * @author david bernard
 */
public class MiscTest {

// TODO not use String.split, it is not consistent across jdk version
// see https://github.com/davidB/scala-maven-plugin/pull/206
//    public void testJdkSplit() throws Exception {
//        assertEquals("Are you using JDK > 7? This failure is expected above JDK 7.", 6, "hello".split("|").length);
//        assertEquals(1, "hello".split("\\|").length);
//        assertEquals(2, "hel|lo".split("\\|").length);
//        assertEquals(3, "hel||lo".split("\\|").length);
//    }

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

      File olderjar = new File(System.getProperty("user.home"), ".m2/repository/net/alchim31/maven/scala-maven-plugin/3.1.0/scala-maven-plugin-3.1.0.jar");
      if (olderjar.exists()) {
        System.out.println("found older jar");
        rScript.addURL(olderjar.toURI().toURL());
        String clname = "scala_maven.ScalaScriptMojo";
        //assertNotSame(s.loadClass(clname), getClass().getClassLoader().loadClass(clname));
        assertNotSame(rScript.loadClass(clname), getClass().getClassLoader().loadClass(clname));
        assertSame(rMojo.loadClass(clname), getClass().getClassLoader().loadClass(clname));
        assertSame(rScript.loadClass(MavenProject.class.getCanonicalName()), MavenProject.class);
        assertSame(rScript.loadClass(MavenSession.class.getCanonicalName()), MavenSession.class);
        assertSame(rScript.loadClass(Log.class.getCanonicalName()), Log.class);
      }
    }
}

package org.scala_tools.maven.executions;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.StringUtils;
import org.scala_tools.maven.StreamLogger;
import org.scala_tools.maven.StreamPiper;
/**
 * Abstract "process builder" object for forked java commands.
 * 
 * @author J. Suereth
 */
public abstract class AbstractForkedJavaCommand extends AbstractJavaMainCaller {
   /**
    * Location of java executable.
    */
   protected String javaExec;
   
   public AbstractForkedJavaCommand(AbstractMojo requester,
         String mainClassName, String classpath, String[] jvmArgs, String[] args)
         throws Exception {
      super(requester, mainClassName, classpath, jvmArgs, args);
      for (String key : System.getenv().keySet()) {
         env.add(key + "=" + System.getenv(key));
     }
     javaExec = System.getProperty("java.home");
     if (javaExec == null) {
         javaExec = System.getenv("JAVA_HOME");
         if (javaExec == null) {
             throw new IllegalStateException("Couldn't locate java, try setting JAVA_HOME environment variable.");
         }
     }
     javaExec += File.separator + "bin" + File.separator + "java";
   }

   protected abstract String[] buildCommand() throws Exception;



   public void run(boolean displayCmd, boolean throwFailure) throws Exception {
      String[] cmd = buildCommand();
      if (displayCmd) {
          requester.getLog().info("cmd: " + " " + StringUtils.join(cmd, " "));
      } else if (requester.getLog().isDebugEnabled()) {
          requester.getLog().debug("cmd: " + " " + StringUtils.join(cmd, " "));
      }
      ProcessBuilder pb = new ProcessBuilder(cmd);
      //pb.directory("myDir");
      if (!logOnly) {
          pb = pb.redirectErrorStream(true);
      }
      Process p = pb.start();
      if (logOnly) {
          new StreamLogger(p.getErrorStream(), requester.getLog(), true).start();
          new StreamLogger(p.getInputStream(), requester.getLog(), false).start();
      } else {
          new StreamPiper(p.getInputStream(), System.out).start();
          new StreamPiper(System.in, p.getOutputStream()).start();
          //new ConsolePiper(p).start();
      }
      int retVal = p.waitFor();
      if (throwFailure && (retVal != 0)) {
          throw new MojoFailureException("command line returned non-zero value:" + retVal);
      }
   }

   public void spawn(boolean displayCmd) throws Exception {
      String[] cmd = buildCommand();
      if (displayCmd) {
          requester.getLog().info("cmd: " + " " + StringUtils.join(cmd, " "));
      } else if (requester.getLog().isDebugEnabled()) {
          requester.getLog().debug("cmd: " + " " + StringUtils.join(cmd, " "));
      }
      ProcessBuilder pb = new ProcessBuilder(cmd);
      pb.start();
   }

}
package org.scala_tools.maven.executions;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.StringUtils;

/**
 * forked java commands.
 *
 * @author D. Bernard
 * @author J. Suereth
 */
public class JavaMainCallerByFork extends JavaMainCallerSupport {
    // //////////////////////////////////////////////////////////////////////////
    // Class
    // //////////////////////////////////////////////////////////////////////////
    public static String toMultiPath(List<String> paths) {
        return StringUtils.join(paths.iterator(), File.pathSeparator);
    }

    public static String toMultiPath(String[] paths) {
        return StringUtils.join(paths, File.pathSeparator);
    }

    public static String[] findFiles(File dir, String pattern) {
        return findFiles(dir, new String[] { pattern }, new String[0]);
    }

    public static String[] findFiles(File dir, String[] includes, String[] excludes) {
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setBasedir(dir);
        scanner.setIncludes(includes);
        scanner.setExcludes(excludes);
        scanner.addDefaultExcludes();
        scanner.scan();
        return scanner.getIncludedFiles();
    }

    public static String toClasspathString(ClassLoader cl) throws Exception {
        if (cl == null) {
            cl = Thread.currentThread().getContextClassLoader();
        }
        StringBuilder back = new StringBuilder();
        while (cl != null) {
            if (cl instanceof URLClassLoader) {
                URLClassLoader ucl = (URLClassLoader) cl;
                URL[] urls = ucl.getURLs();
                for (URL url : urls) {
                    if (back.length() != 0) {
                        back.append(File.pathSeparatorChar);
                    }
                    back.append(url.getFile());
                }
            }
            cl = cl.getParent();
        }
        return back.toString();
    }

    // //////////////////////////////////////////////////////////////////////////
    // Object
    // //////////////////////////////////////////////////////////////////////////

    private boolean _forceUseArgFile = false;

   /**
    * Location of java executable.
    */
   private String _javaExec;

   public JavaMainCallerByFork(AbstractMojo requester, String mainClassName, String classpath, String[] jvmArgs, String[] args, boolean forceUseArgFile) throws Exception {
      super(requester, mainClassName, classpath, jvmArgs, args);
      for (String key : System.getenv().keySet()) {
         env.add(key + "=" + System.getenv(key));
     }
     _javaExec = System.getProperty("java.home");
     if (_javaExec == null) {
         _javaExec = System.getenv("JAVA_HOME");
         if (_javaExec == null) {
             throw new IllegalStateException("Couldn't locate java, try setting JAVA_HOME environment variable.");
         }
     }
     _javaExec += File.separator + "bin" + File.separator + "java";
     _forceUseArgFile = forceUseArgFile;
   }

   public void run(boolean displayCmd, boolean throwFailure) throws Exception {
      String[] cmd = buildCommand();
      if (displayCmd) {
          requester.getLog().info("cmd: " + " " + StringUtils.join(cmd, " "));
      } else if (requester.getLog().isDebugEnabled()) {
          requester.getLog().debug("cmd: " + " " + StringUtils.join(cmd, " "));
      }
      Executor exec = new DefaultExecutor();
      CommandLine cl = new CommandLine(cmd[0]);
      for(int i = 1; i < cmd.length; i++) {
          cl.addArgument(cmd[i]);
      }
      int exitValue = exec.execute(cl);

      if (throwFailure && (exitValue != 0)) {
          throw new MojoFailureException("command line returned non-zero value:" + exitValue);
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

   protected String[] buildCommand() throws Exception {
       ArrayList<String> back = new ArrayList<String>(2 + jvmArgs.size() + args.size());
       back.add(_javaExec);
       if (!_forceUseArgFile && (lengthOf(args, 1) + lengthOf(jvmArgs, 1) < 400)) {
           back.addAll(jvmArgs);
           back.add(mainClassName);
           back.addAll(args);
       } else {
           File jarPath = new File(MainHelper.locateJar(MainHelper.class));
           requester.getLog().debug("plugin jar to add :" + jarPath);
           addToClasspath(jarPath);
           back.addAll(jvmArgs);
           back.add(MainWithArgsInFile.class.getName());
           back.add(mainClassName);
           back.add(MainHelper.createArgFile(args).getCanonicalPath());
       }
       return back.toArray(new String[back.size()]);
   }

   private long lengthOf(List<String> l, long sepLength) throws Exception {
       long back = 0;
       for(String str : l) {
           back += str.length() + sepLength;
       }
       return back;
   }
}
package org.scala_tools.maven.executions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.apache.maven.plugin.AbstractMojo;

/**
 * Creates an external process which executes a scala command.  The arguments to the scala command will be in a file.
 * 
 * @author J. Suereth
 *
 */
public class ScalaCommandWIthArgsInFile extends AbstractForkedJavaCommand {

   

   public ScalaCommandWIthArgsInFile(AbstractMojo requester,
         String mainClassName, String classpath, String[] jvmArgs, String[] args)
         throws Exception {
      super(requester, mainClassName, classpath, jvmArgs, args);    
   }

   private File createArgFile() throws IOException {
      final File argFile = File.createTempFile("scala-maven-", ".args");
      argFile.deleteOnExit();
      final PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(argFile)));
      try {
         for(String arg : args) {
            out.println(arg);
         }
      } finally {
         out.close();
      }
      return argFile;
   }
   
   @Override
   protected String[] buildCommand() throws IOException {
      ArrayList<String> back = new ArrayList<String>(3 + jvmArgs.size());
      back.add(javaExec);
      back.addAll(jvmArgs);
      back.add(mainClassName);
      back.add("@" + createArgFile().getAbsolutePath());
      return back.toArray(new String[back.size()]);
  }

}

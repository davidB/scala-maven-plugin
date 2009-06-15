package org.scala_tools.maven.executions;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.maven.plugin.AbstractMojo;

/**
 * Creates an external process which executes a scala command.  The arguments to the scala command will be in a file.
 * 
 * @author J. Suereth
 *
 */
public class ScalaCommandWIthArgsInFile extends AbstractForkedJavaCommand {

   public ScalaCommandWIthArgsInFile(AbstractMojo requester, String mainClassName, String classpath, String[] jvmArgs, String[] args) throws Exception {
      super(requester, mainClassName, classpath, jvmArgs, args);    
   }
   
   @Override
   protected String[] buildCommand() throws IOException {
      ArrayList<String> back = new ArrayList<String>(3 + jvmArgs.size());
      back.add(javaExec);
      back.addAll(jvmArgs);
      back.add(mainClassName);
      String fileName = MainHelper.createArgFile(args).getCanonicalPath();
      back.add("@" + fileName);
      return back.toArray(new String[back.size()]);
  }

}

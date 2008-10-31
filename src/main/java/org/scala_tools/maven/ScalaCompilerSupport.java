/*
 * Copyright 2007 scala-tools.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org.scala_tools.maven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.codehaus.plexus.util.FileUtils;

/**
 * Abstract parent of all Scala Mojo
 */
public abstract class ScalaCompilerSupport extends ScalaMojoSupport {
    /**
     * Pause duration between to scan to detect changed file to compile.
     * Used only if compileInLoop or testCompileInLoop is true.
     */
    protected long loopSleep = 2500;


    abstract protected File getOutputDir() throws Exception;

    abstract protected List<String> getClasspathElements() throws Exception;
    /**
     * Retreives the list of *all* root source directories.  We need to pass all .java and .scala files into the scala compiler
     */
    abstract protected List<String> getSourceDirectories() throws Exception;
    
    @Override
    protected void doExecute() throws Exception {
        File outputDir = normalize(getOutputDir());
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        for(String directory : getSourceDirectories()) {
        	getLog().error(directory);
        }
        int nbFiles = compile(getSourceDirectories(), outputDir, getClasspathElements(), false);
        switch (nbFiles) {
            case -1:
                getLog().warn("No source files found.");
                break;
            case 0:
                getLog().info("Nothing to compile - all classes are up to date");;
                break;
            default:
                break;
        }
    }

    protected File normalize(File f) {
        try {
            f = f.getCanonicalFile();
        } catch (IOException exc) {
            f = f.getAbsoluteFile();
        }
        return f;
    }
    
    
    protected int compile(File sourceDir, File outputDir, List<String> classpathElements, boolean compileInLoop) throws Exception, InterruptedException {
    	getLog().warn("Using older form of compile");
    	return compile(Arrays.asList(sourceDir.getAbsolutePath()), outputDir, classpathElements, compileInLoop);
    }
    
   protected int compile(List<String> sourceRootDirs, File outputDir, List<String> classpathElements, boolean compileInLoop) throws Exception, InterruptedException {
       List<String> scalaSourceFiles = findSource(sourceRootDirs, "scala");
       if (scalaSourceFiles.size() == 0) {
           return -1;
       }

       // filter uptodate
       File lastCompileAtFile = new File(outputDir + ".timestamp");
       long lastCompileAt = lastCompileAtFile.lastModified();
       ArrayList<File> files = new ArrayList<File>(scalaSourceFiles.size());
       for (String x : scalaSourceFiles) {
           File f = new File(x);
           if (f.lastModified() >= lastCompileAt) {
               files.add(f);
           }
       }
       if (files.size() == 0) {
           return 0;
       }
       //Add java files to the source, so we make sure we can have nested dependencies
       //BUT only when not compiling in "loop" fashion
       if(!compileInLoop) {
    	   List<String> javaSourceFiles = findSource(sourceRootDirs,"java");
    	   for(String javaSourceFile : javaSourceFiles) {
    		   files.add(new File(javaSourceFile));
    	   }
       }
       
       if (!compileInLoop) {
           getLog().info(String.format("Compiling %d source files to %s", files.size(), outputDir.getAbsolutePath()));
       }
       long now = System.currentTimeMillis();
       JavaCommand jcmd = getScalaCommand();
       jcmd.addArgs("-classpath", JavaCommand.toMultiPath(classpathElements));
       jcmd.addArgs("-d", outputDir.getAbsolutePath());
       //jcmd.addArgs("-sourcepath", sourceDir.getAbsolutePath());
       for (File f : files) {
           jcmd.addArgs(f.getAbsolutePath());
           if (compileInLoop) {
               getLog().info(String.format("%tR compiling %s", now, f.getName()));
           }
       }
       jcmd.run(displayCmd, !compileInLoop);
       if (lastCompileAtFile.exists()) {
           lastCompileAtFile.setLastModified(now);
       } else {
           FileUtils.fileWrite(lastCompileAtFile.getAbsolutePath(), ".");
       }
       return files.size();
   }
   /**
    * Finds all source files in a set of directories with a given extension. 
    */
   private List<String> findSource(List<String> sourceRootDirs, String extension) {
	   List<String> sourceFiles = new ArrayList<String>();
	   //TODO - Since we're making files anyway, perhaps we should just test for existence here...
	   for(String rootSourceDir : normalizeSourceRoots(sourceRootDirs)) {
		   File dir = normalize(new File(rootSourceDir));	
		   String[] tmpFiles = JavaCommand.findFiles(dir, "**/*." + extension);
		   for(String tmpLocalFile : tmpFiles) {			   
			   File tmpAbsFile = normalize(new File(dir, tmpLocalFile));
			   sourceFiles.add(tmpAbsFile.getAbsolutePath());
		   }
	   }
	   return sourceFiles;
   }
   
    
    
    /**
     * This limits the source directories to only those that exist for real.
     */
    private List<String> normalizeSourceRoots( List<String> compileSourceRootsList )
    {
        List<String> newCompileSourceRootsList = new ArrayList<String>();
        if ( compileSourceRootsList != null )
        {
            // copy as I may be modifying it
            for ( String srcDir : compileSourceRootsList )
            {
            	File srcDirFile = normalize(new File(srcDir));
                if ( !newCompileSourceRootsList.contains( srcDirFile.getAbsolutePath() ) && srcDirFile.exists() )
                {
                    newCompileSourceRootsList.add( srcDirFile.getAbsolutePath() );
                }
            }
        }
        return newCompileSourceRootsList;
    }
}

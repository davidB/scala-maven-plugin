package org.scala_tools.maven.executions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * @author David Bernard
 */
public class MainHelper {
    //------------------------------ utils / shared function

    /**
     * Escapes arguments as necessary so the StringTokenizer for scala arguments pulls in filenames with spaces correctly.
     * @param arg
     * @return
     */
    private static String escapeArgumentForScalacArgumentFile(String arg) {
        if(arg.matches(".*\\s.*")) {
            return '"' + arg + '"';
        }
        return arg;
    }
    
    /**
     * UnEscapes arguments as necessary so the StringTokenizer for scala arguments pulls in filenames with spaces correctly.
     * @param arg
     * @return
     */
    private static String unescapeArgumentForScalacArgumentFile(String arg) {
        if(arg.charAt(0) == '"' && arg.charAt(arg.length() -1) == '"') {
            return arg.substring(1, arg.length() -1);
        }
        return arg;
    }

    /**
     * Creates a file containing all the arguments. This file has a very simple format of argument (white-space argument).
     * 
     * @return
     * @throws IOException
     */
    public static File createArgFile(List<String> args) throws IOException {
       final File argFile = File.createTempFile("scala-maven-", ".args");
       //argFile.deleteOnExit();
       final PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(argFile)));
       try {
          for(String arg : args) {
             out.println(escapeArgumentForScalacArgumentFile(arg));
          }
       } finally {
          out.close();
       }
       return argFile;
    }
    
    /**
     * Creates a file containing all the arguments. This file has a very simple format of argument (white-space argument).
     * 
     * @return
     * @throws IOException
     */
    public static List<String> readArgFile(File argFile) throws IOException {
       ArrayList<String> back = new ArrayList<String>();
       final BufferedReader in = new BufferedReader(new FileReader(argFile));
       try {
          String line = null;
          while ((line = in.readLine()) != null) {
             back.add(unescapeArgumentForScalacArgumentFile(line));
          }
       } finally {
          in.close();
       }
       return back;
    }

    /** Runs the main method of a java class */
    public static void runMain(String mainClassName, List<String> args, ClassLoader cl) throws Exception {
        if(cl == null) {
            cl = Thread.currentThread().getContextClassLoader();
        }
        Class<?> mainClass = cl.loadClass(mainClassName);
        Method mainMethod = mainClass.getMethod("main", String[].class);        
        int mods = mainMethod.getModifiers();
        if(mainMethod.getReturnType() != void.class || !Modifier.isStatic(mods) || !Modifier.isPublic(mods)) {
            throw new NoSuchMethodException("main");
        }
        String[] argArray = args.toArray(new String[args.size()]);
        
        //TODO - Redirect System.in System.err and System.out
        
        mainMethod.invoke(null, new Object[] {argArray});
    }
    
    public static String locateJar(Class<?> c ) throws ClassNotFoundException {
        final URL location;
        final String classLocation = c.getName().replace('.', '/') + ".class";
        final ClassLoader loader = c.getClassLoader();
        if( loader == null ) {
            location = ClassLoader.getSystemResource(classLocation);
        } else {
            location = loader.getResource(classLocation);
        }
        if( location != null ){
            Pattern p = Pattern.compile( "^.*:(.*)!.*$" ) ;
            Matcher m = p.matcher( location.toString() ) ;
            if( m.find() ) {
                return m.group( 1 ) ;
            } else {
                throw new ClassNotFoundException( "Cannot parse location of '" + location + "'.  Probably not loaded from a Jar" ) ;
            }
        } else { 
            throw new ClassNotFoundException( "Cannot find class '" + c.getName() + " using the classloader" ) ;
        }
    }
}

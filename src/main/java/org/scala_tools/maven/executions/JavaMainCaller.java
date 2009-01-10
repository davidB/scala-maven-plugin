package org.scala_tools.maven.executions;

import java.io.File;
/**
 * This interface is used to create a call on a main method of a java class.
 * 
 * The important implementations are JavaCommand and ReflectionJavaCaller
 * 
 * @author J. Suereth
 *
 */
public interface JavaMainCaller {
	/** Adds an environemnt variable */
	public abstract void addEnvVar(String key, String value);
	/** Adds a JVM arg.  Note: This is not available for in-process "forks" */
	public abstract void addJvmArgs(String... args);
	/** Adds arguments for the process */
	public abstract void addArgs(String... args);
	/** Adds option (basically two arguments) */
	public abstract void addOption(String key, String value);
	/** Adds an option (key-file pair). This will pull the absolute path of the file */
	public abstract void addOption(String key, File value);
	/** Adds the key iff the value is true */
	public abstract void addOption(String key, boolean value);
	/** Sets the process to *only* log errors */
	public abstract void setLogOnly(boolean v);

	// TODO: avoid to have several Thread to pipe stream
	// TODO: add support to inject startup command and shutdown command (on :quit)
	public abstract void run(boolean displayCmd) throws Exception;
	/** Runs the JavaMain with all the built up arguments/options */
	public abstract void run(boolean displayCmd, boolean throwFailure)
			throws Exception;

	/**
	 * run the command without stream redirection nor waiting for exit
	 *
	 * @param displayCmd
	 * @throws Exception
	 */
	public abstract void spawn(boolean displayCmd) throws Exception;

}
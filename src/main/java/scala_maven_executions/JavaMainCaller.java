/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package scala_maven_executions;

import java.io.File;

/**
 * This interface is used to create a call on a main method of a java class.
 *
 * <p>The important implementations are JavaCommand and ReflectionJavaCaller
 *
 * @author J. Suereth
 */
public interface JavaMainCaller {
  /** Adds a JVM arg. Note: This is not available for in-process "forks" */
  void addJvmArgs(String... args);

  /** Adds arguments for the process */
  void addArgs(String... args);

  /** Adds option (basically two arguments) */
  void addOption(String key, String value);

  /** Adds an option (key-file pair). This will pull the absolute path of the file */
  void addOption(String key, File value);

  /** Adds the key if the value is true */
  void addOption(String key, boolean value);

  /** request run to be redirected to maven/requester logger */
  void redirectToLog();

  // TODO: avoid to have several Thread to pipe stream
  // TODO: add support to inject startup command and shutdown command (on :quit)
  void run(boolean displayCmd) throws Exception;

  /** Runs the JavaMain with all the built up arguments/options */
  boolean run(boolean displayCmd, boolean throwFailure) throws Exception;

  /**
   * run the command without stream redirection nor waiting for exit
   *
   * @param displayCmd
   * @return the spawn Process (or null if no process was spawned)
   * @throws Exception
   */
  SpawnMonitor spawn(boolean displayCmd) throws Exception;
}

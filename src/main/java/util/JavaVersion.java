/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package util;

public final class JavaVersion {

  private JavaVersion() {}

  public static final int JAVA_MAJOR_VERSION;

  static {
    String javaSpecVersion = System.getProperty("java.specification.version");
    String[] components = javaSpecVersion.split("\\.");
    int component0 = Integer.parseInt(components[0]);
    JAVA_MAJOR_VERSION = component0 == 1 ? Integer.parseInt(components[1]) : component0;
  }
}

/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package scala_maven;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class VersionNumberTest {

  @Test
  public void compare() throws Exception {
    assertEquals(1, new VersionNumber("2.7.1").compareTo(new VersionNumber("1.0")));
    assertEquals(1, new VersionNumber("2.7.1").compareTo(new VersionNumber("1.9")));
    assertEquals(1, new VersionNumber("2.7.1").compareTo(new VersionNumber("2.0")));
    assertEquals(1, new VersionNumber("2.7.1").compareTo(new VersionNumber("2.7")));
    assertEquals(1, new VersionNumber("2.7.1").compareTo(new VersionNumber("2.7-rc")));
    assertEquals(1, new VersionNumber("2.7.1").compareTo(new VersionNumber("2.7.0")));
    assertEquals(0, new VersionNumber("2.7.1").compareTo(new VersionNumber("2.7.1")));
    assertEquals(-1, new VersionNumber("2.7.1").compareTo(new VersionNumber("2.7.2-rc1")));
    assertEquals(-1, new VersionNumber("2.7.1").compareTo(new VersionNumber("2.8")));
    assertEquals(-1, new VersionNumber("2.7.1").compareTo(new VersionNumber("3.0")));
  }

  @Test
  public void max() throws Exception {
    assertEquals(
        new VersionNumber("2.7.1"), new VersionNumber("2.7.1").max(new VersionNumber("1.0")));
    assertEquals(
        new VersionNumber("2.7.1"), new VersionNumber("2.7.1").max(new VersionNumber("1.9")));
    assertEquals(
        new VersionNumber("2.7.1"), new VersionNumber("2.7.1").max(new VersionNumber("2.0")));
    assertEquals(
        new VersionNumber("2.7.1"), new VersionNumber("2.7.1").max(new VersionNumber("2.7")));
    assertEquals(
        new VersionNumber("2.7.1"), new VersionNumber("2.7.1").max(new VersionNumber("2.7-rc")));
    assertEquals(
        new VersionNumber("2.7.1"), new VersionNumber("2.7.1").max(new VersionNumber("2.7.0")));
    assertEquals(
        new VersionNumber("2.7.1"), new VersionNumber("2.7.1").max(new VersionNumber("2.7.1")));
    assertEquals(
        new VersionNumber("2.7.2-rc1"),
        new VersionNumber("2.7.1").max(new VersionNumber("2.7.2-rc1")));
    assertEquals(
        new VersionNumber("2.8"), new VersionNumber("2.7.1").max(new VersionNumber("2.8")));
    assertEquals(
        new VersionNumber("3.0"), new VersionNumber("2.7.1").max(new VersionNumber("3.0")));
  }

  @Test
  public void parse() throws Exception {
    assertParseVN("2.7.1", 2, 7, 1, null);
    assertParseVN("2.7", 2, 7, 0, null);
    assertParseVN("2.7.1.RC", 2, 7, 1, ".RC");
    assertParseVN("2.7.1.RC", 2, 7, 1, ".RC");
    assertParseVN("2.7.RC", 2, 7, 0, ".RC");
    assertParseVN("2.7-RC", 2, 7, 0, "-RC");
    assertParseVN("2.7.1-SNAPSHOT", 2, 7, 1, "-SNAPSHOT");
  }

  @Test
  public void compareMask() throws Exception {
    assertEquals(1, new VersionNumberMask("2.7").compareTo(new VersionNumber("1.0")));
    assertEquals(1, new VersionNumberMask("2.7").compareTo(new VersionNumber("1.9")));
    assertEquals(1, new VersionNumberMask("2.7").compareTo(new VersionNumber("2.0")));
    assertEquals(0, new VersionNumberMask("2.7").compareTo(new VersionNumber("2.7")));
    assertEquals(0, new VersionNumberMask("2.7").compareTo(new VersionNumber("2.7-rc")));
    assertEquals(0, new VersionNumberMask("2.7").compareTo(new VersionNumber("2.7.0")));
    assertEquals(0, new VersionNumberMask("2.7").compareTo(new VersionNumber("2.7.1")));
    assertEquals(0, new VersionNumberMask("2.7").compareTo(new VersionNumber("2.7.2-rc1")));
    assertEquals(-1, new VersionNumberMask("2.7").compareTo(new VersionNumber("2.8")));
    assertEquals(-1, new VersionNumberMask("2.7").compareTo(new VersionNumber("3.0")));
    assertEquals(1, new VersionNumber("2.7-rc").compareTo(new VersionNumberMask("2.7")));
    assertEquals(1, new VersionNumber("2.7.0").compareTo(new VersionNumberMask("2.7")));
    assertEquals(1, new VersionNumber("2.7.1").compareTo(new VersionNumberMask("2.7")));
    assertEquals(1, new VersionNumber("2.7.2-rc1").compareTo(new VersionNumberMask("2.7")));
  }

  private void assertParseVN(String str, int major, int minor, int bugfix, String modifier) {
    VersionNumber v = new VersionNumber(str);
    assertEquals("test major of " + str, major, v.major);
    assertEquals("test minor of " + str, minor, v.minor);
    assertEquals("test bugfix of " + str, bugfix, v.bugfix);
    assertEquals("test modifier of " + str, modifier, v.modifier);
  }
}

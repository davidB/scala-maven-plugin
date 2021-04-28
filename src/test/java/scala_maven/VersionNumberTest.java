
/*
 * Copyright 2011-2020 scala-maven-plugin project (https://davidb.github.io/scala-maven-plugin/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

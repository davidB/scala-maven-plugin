package org.scala_tools.maven;

import org.scala_tools.maven.VersionNumber;

import junit.framework.TestCase;

public class VersionNumberTest extends TestCase {
    public void testCompare() throws Exception {
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
}

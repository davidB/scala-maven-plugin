/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package scala_maven;

import junit.framework.TestCase;
import org.codehaus.plexus.util.StringUtils;

/**
 *
 * @author david bernard
 */
public class MiscTest extends TestCase {
    public void testJdkSplit() throws Exception {
        assertEquals(6, "hello".split("|").length);
        assertEquals(1, "hello".split("\\|").length);
        assertEquals(2, "hel|lo".split("\\|").length);
        assertEquals(3, "hel||lo".split("\\|").length);
    }

    public void testStringUtilsSplit() throws Exception {
        assertEquals(1, StringUtils.split("hello", "|").length);
        assertEquals(1, StringUtils.split("hello|", "|").length);
        assertEquals(2, StringUtils.split("hel|lo", "|").length);
        assertEquals(2, StringUtils.split("hel||lo", "|").length);
    }
}

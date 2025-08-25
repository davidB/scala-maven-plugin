/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package util;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.Lists;
import java.io.File;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class FileUtilsTest {
  @Test
  public void fromStrings_should_preserve_insertion_order() {
    List<String> expected =
        Lists.newArrayList("5.jar", "23.jar", "56.jar", "2.jar", "48.jar", "99.jar", "1234.jar");
    Set<File> files = FileUtils.fromStrings(expected);
    List<String> actual = Lists.newArrayList();
    for (File file : files) {
      actual.add(file.getName());
    }
    assertEquals(actual, expected);
  }
}

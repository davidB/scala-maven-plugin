/*
 * This is free and unencumbered software released into the public domain.
 * See UNLICENSE.
 */
package scala_maven;

import java.io.File;
import java.util.List;

public class Classpath {
  private List<File> add;

  public List<File> getAdd() {
    return add;
  }

  public void setAdd(List<File> add) {
    this.add = add;
  }
}

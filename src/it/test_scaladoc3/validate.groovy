try {
  def file = new File(basedir, 'target/scaladoc-jar-test-1.0-SNAPSHOT-javadoc.jar')
  assert file.exists()

  return true

} catch(Throwable e) {
  e.printStackTrace()
  return false
}

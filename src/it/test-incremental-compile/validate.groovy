try {

  def a = new File(basedir, 'target/classes/A.class')
  assert a.exists()

  def b = new File(basedir, 'target/classes/B.class')
  assert b.exists()

  def x = new File(basedir, 'target/test-classes/X.class')
  assert x.exists()

  def y = new File(basedir, 'target/test-classes/Y.class')
  assert y.exists()

  def analysis = new File(basedir, 'analysis')
  assert analysis.exists()

  def testAnalysis = new File(basedir, 'test-analysis')
  assert testAnalysis.exists()

  return true

} catch(Throwable e) {
  e.printStackTrace()
  return false
}

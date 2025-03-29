try {

def mainFile = new File(basedir, 'target/classes/Main.class')
assert !mainFile.exists()

def testFile = new File(basedir, 'target/test-classes/Test.class')
assert testFile.exists()

return true

} catch(Throwable e) {
  e.printStackTrace()
  return false
}

try {

def file = new File(basedir, 'target/classes/MyClass$.class')
assert file.exists()

def file1 = new File(basedir, 'target/classes/KTestClass.class')
assert file1.exists()

return true

} catch(Throwable e) {
  e.printStackTrace()
  return false
}
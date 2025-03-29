try {

def file = new File(basedir, 'target/classes2/MyClass.class')
assert file.exists()

def file2 = new File(basedir, 'target/classes2/MyClass$.class')
assert file2.exists()

def file3 = new File(basedir, 'target/classes/TestClass.class')
assert file3.exists()

return true

} catch(Throwable e) {
  e.printStackTrace()
  return false
}
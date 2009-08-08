try {

def file = new File(basedir, 'target/classes/MyClass.class')
assert file.exists()

def file2 = new File(basedir, 'target/classes/MyClass$.class')
assert file2.exists()

def file3 = new File(basedir, 'target/classes/TestClass.class')
assert file3.exists()

def file4 = new File(basedir, 'target/site/scaladocs/MyClass.html')
assert file4.exists()


return true

} catch(Throwable e) {
  e.printStackTrace()
  return false
}
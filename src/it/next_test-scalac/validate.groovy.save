try {

def file = new File(basedir, 'target/classes/MyClass.class')
assert file.exists()

def file2 = new File(basedir, 'target/classes/MyClass$.class')
assert file2.exists()

return true

} catch(Throwable e) {
  e.printStackTrace()
  return false
}

try {

def file2 = new File(basedir, 'target/classes/example/Foo')
assert file2.exists()

def file3 = new File(basedir, 'target/classes/example/Foo.class')
assert file3.exists()

return true

} catch(Throwable e) {
  e.printStackTrace()
  return false
}
try {

def fileI = new File(basedir, 'target/hello-from-inline.txt')
assert fileI.exists()

def fileE = new File(basedir, 'target/hello-from-external.txt')
assert fileE.exists()

return true

} catch(Throwable e) {
  e.printStackTrace()
  return false
}
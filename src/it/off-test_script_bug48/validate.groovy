try {

def file = new File(basedir, 'target/deps.txt')
assert file.exists()



return true

} catch(Throwable e) {
  e.printStackTrace()
  return false
}

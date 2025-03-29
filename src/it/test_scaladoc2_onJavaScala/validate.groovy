try {

assert new File(basedir, 'target/site/scaladocs/HelloJava.html').exists()
//assert new File(basedir, 'target/site/scaladocs/HelloJava$.html').exists()
assert new File(basedir, 'target/site/scaladocs/HelloScala$.html').exists()
assert new File(basedir, 'target/site/scaladocs/index.html').exists()
//assert file4.exists()


return true

} catch(Throwable e) {
  e.printStackTrace()
  return false
}

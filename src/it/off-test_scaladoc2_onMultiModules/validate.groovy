try {

assert new File(basedir, 'm1/target/site/scaladocs/p1/MyClass.html').exists()
assert new File(basedir, 'm2/target/site/scaladocs/p2/MyClass.html').exists()
assert new File(basedir, 'target/site/scaladocs/p1/MyClass.html').exists()
assert new File(basedir, 'target/site/scaladocs/p2/MyClass.html').exists()
return true

} catch(Throwable e) {
  e.printStackTrace()
  return false
}

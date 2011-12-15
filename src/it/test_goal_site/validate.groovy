try {

assert new File(basedir, 'target/classes/MyClass.class').exists()

assert new File(basedir, 'target/classes/MyClass$.class').exists()

assert new File(basedir, 'target/classes/TestClass.class').exists()

assert new File(basedir, 'target/site/scaladocs/MyClass.html').exists()

return true

} catch(Throwable e) {
  e.printStackTrace()
  return false
}



try {
  def modA = new File(basedir, 'mod-a/target/classes/a/A.class')
  def modB = new File(basedir, 'mod-b/target/classes/b/B.class')
  assert modA.exists() : "mod-a did not compile"
  assert modB.exists() : "mod-b did not compile"

  // The real assertion: the compiler bridge is built once and reused for the second module,
  // instead of being rebuilt from scratch for every module in the reactor.
  def logFile = new File(basedir, "build.log")
  def builtCount = 0
  def reusedCount = 0
  logFile.eachLine { line ->
    if (line.contains("Built Scala") && line.contains("compiler bridge")) {
      builtCount++
    }
    if (line.contains("Reusing cached Scala") && line.contains("compiler bridge")) {
      reusedCount++
    }
  }
  assert builtCount == 1 : "expected the compiler bridge to be built exactly once, was built $builtCount times"
  assert reusedCount == 1 : "expected the compiler bridge to be reused exactly once, was reused $reusedCount times"

  return true
} catch (Throwable e) {
  e.printStackTrace()
  return false
}

try {
  def modA = new File(basedir, 'mod-a/target/classes/a/A.class')
  def modB = new File(basedir, 'mod-b/target/classes/b/B.class')
  assert modA.exists() : "mod-a did not compile"
  // The real assertion: the reactor reached and built the SECOND module. Without the non-exiting
  // process() entry point, module 'a' System.exit()s the Maven JVM and 'b' is never compiled.
  assert modB.exists() : "mod-b was NOT compiled — reactor did not survive module 'a' (in-process System.exit)"
  return true
} catch(Throwable e) { e.printStackTrace(); return false }

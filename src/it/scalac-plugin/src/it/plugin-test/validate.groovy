try {

	def file = new File(basedir, 'target/classes/test-plugin.out')
	assert file.exists()
	
	
	return true

} catch(Throwable e) {
  e.printStackTrace()
  return false
}
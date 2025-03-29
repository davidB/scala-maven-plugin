def file = new File(basedir, 'target')

if (file.exists()){
  def targetEmpty = file.listFiles()
          .findAll { it.name != "project-local-repo" }
          .isEmpty()

  return targetEmpty
}

return true

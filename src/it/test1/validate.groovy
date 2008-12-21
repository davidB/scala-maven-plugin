def file = new File(basedir, 'target/classes/MyClass.class')
assert file.exists()

def file2 = new File(basedir, 'target/classes/MyClass$.class')
assert file2.exists()

def file3 = new File(basedir, 'target/classes/TestClass.class')
assert file3.exists()
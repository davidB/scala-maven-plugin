

def className = "TestJava"


def makeJavaClass =  { name ->
	def file = new File(basedir, "/src/main/java/" + name + ".java");
	if(!file.exists()) {
		file.createNewFile();
		file.withPrintWriter({ output ->
			output.println("class " + name + " {}");			
		});
	}
}

new File(basedir, "/src/main/java").mkdirs();

makeJavaClass("TestJavaClass");

(1..512).each({ value ->
   makeJavaClass("TestJavaClass" + value);
});

assert true
true
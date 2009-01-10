

def className = "TestJava"


def makeJavaClass =  { name ->
	def file = new File(basedir, "/src/main/java/" + name + ".java");
	if(!file.exists()) {	
		file.withPrintWriter({ output ->
			output.println("class " + name + " {}");			
		});
	}
}

makeJavaClass("TestJavaClass");

(1..512).each({ value ->
   makeJavaClass("TestJavaClass" + value);
});

assert true
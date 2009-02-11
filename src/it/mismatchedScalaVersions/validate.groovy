def echoString = "OUT: [WARNING] Multiple Scala versions detected!"
def logFile = new File(basedir, "build.log")
//Look for echo string
def found = false;
logFile.eachLine({ line ->
   if(line.startsWith(echoString)) {
     found = true;
   }
});

assert found

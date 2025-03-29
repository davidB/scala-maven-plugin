try {

def echoString = "[WARNING] Multiple versions of scala libraries detected!"
def logFile = new File(basedir, "build.log")
//Look for echo string
def found = false;
logFile.eachLine({ line ->
   if(line.contains(echoString)) {
     found = true;
   }
});

assert found


return true

} catch(Throwable e) {
  e.printStackTrace()
  return false
}

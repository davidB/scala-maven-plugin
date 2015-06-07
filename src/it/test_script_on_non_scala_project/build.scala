import java.io.PrintWriter
println("hello from build.scala ")
val fo = new PrintWriter("target/hello-from-external.txt")
try {
  fo.println("Hello")
} finally {
  fo.close()
}

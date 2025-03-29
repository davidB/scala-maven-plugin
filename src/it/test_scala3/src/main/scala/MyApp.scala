@main
def Main(args: String*): Unit =
  println("Happy Birthday " + twice(3))
end Main

def twice(x: Int): Int = x * 2
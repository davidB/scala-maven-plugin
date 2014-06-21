object HelloScala extends App {
  val hj = new HelloJava
  hj.sayHello

  def sayHello = println("Scala says: Hello Java!")
}
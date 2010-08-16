object HelloScala extends Application {
  val hj = new HelloJava
  hj.sayHello

  def sayHello = println("Scala says: Hello Java!")  
}
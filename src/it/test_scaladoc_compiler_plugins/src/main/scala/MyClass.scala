import scala.language.higherKinds

object MyClass {

  def testWithHigherKinded[Either[Int, +*]] = ???
}

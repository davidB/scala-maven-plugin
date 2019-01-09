import scala.language.higherKinds

object MyClass {

  class TestKinded[F[_], G]

  def testWithHigherKinded[F[_] : TestKinded[?[_], Int]] = ???

}

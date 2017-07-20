import scala.reflect.macros.Context

// uses the technique described in:
// http://docs.scala-lang.org/overviews/macros/overview.html#writing_bigger_macros
class Helper[C <: Context](val c: C) extends QuasiquoteCompat {
  import c.universe._

  // to learn more about quasiquotes, visit:
  // http://docs.scala-lang.org/overviews/macros/quasiquotes.html
  def hello = q"""
    println("hello world!")
  """
}
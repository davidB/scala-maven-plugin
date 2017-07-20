import language.experimental.macros
import scala.reflect.macros.Context

object Macros {
  def hello = macro impl
  def impl(c: Context) = {
    // uses the technique described in:
    // http://docs.scala-lang.org/overviews/macros/overview.html#writing_bigger_macros
    val helper = new Helper[c.type](c)
    c.Expr[Unit](helper.hello)
  }
}
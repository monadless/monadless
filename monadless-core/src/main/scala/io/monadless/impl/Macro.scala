package io.monadless.impl

import scala.reflect.macros.blackbox.Context
import scala.reflect.macros.TypecheckException

private[monadless] class Macro(val c: Context) {
  import c.universe._

  def lift[M[_], T](body: Expr[T])(implicit m: WeakTypeTag[M[_]]): Tree = {
    val tree = Transformer[M](c)(body.tree)
    Trees.traverse(c)(tree) {
      case tree @ q"$pack.unlift[$t]($v)" =>
        c.error(tree.pos, "Unsupported unlift position")
    }
    try c.typecheck(tree)
    catch {
      case e: TypecheckException =>
        val msg =
          s"""Can't typecheck the monadless transformation. Please file a bug report with this error and your `Monadless` instance. 
             |Failure: ${e.msg}
             |Tree: $tree""".stripMargin
        c.abort(c.enclosingPosition, msg)
    }
  }
}
package io.monadless.impl

import scala.reflect.macros.blackbox.Context
import scala.language.higherKinds

private[monadless] class Macro(val c: Context) {
  import c.universe._

  def lift[M[_], T](body: Expr[T])(implicit m: WeakTypeTag[M[_]]): Tree = {
    val tree = Transformer[M](c)(body.tree)
    Trees.traverse(c)(tree) {
      case tree @ q"$pack.unlift[$t]($v)" =>
        c.error(tree.pos, "Unsupported unlift position")
    }
    tree
  }
}
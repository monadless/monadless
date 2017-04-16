package io.monadless.impl

import scala.reflect.macros.blackbox.Context

private[monadless] class Macro(val c: Context) {
  import c.universe._

  def lift[T](body: Expr[T]): Tree = {
    val tree = Transformer(c)(body.tree)
    Trees.traverse(c)(tree) {
      case tree @ q"$pack.unlift[$t]($v)" =>
        c.error(tree.pos, "Unsupported unlift position")
    }
    tree
  }
}
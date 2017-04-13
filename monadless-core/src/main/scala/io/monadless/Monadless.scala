package io.monadless

import scala.reflect.macros.blackbox.Context
//import scala.annotation.compileTimeOnly
import language.experimental.macros
import language.higherKinds

trait Monadless[M[_]] {

  /* "ghost" methods 

    def apply[T](v: => T): M[T]
    def join[T1, T2](m1: M[T1], m2: M[T2]): M[(T1, T2)]
    def get[T](m: M[T]): T
    def handle[T](m: M[T])(pf: PartialFunction[Throwable, T]): M[T]
    def rescue[T](m: M[T])(pf: PartialFunction[Throwable, M[T]]): M[T]
    def ensure[T](m: M[T])(f: => Unit): M[T]
  
  */

  def lift[T](body: T): M[T] = macro Macro.lift[M, T]

  //  @compileTimeOnly("`unlift` must be used within `lift`")
  def unlift[T](m: M[T]): T = ???
}

private[monadless] class Macro(val c: Context) {
  import c.universe._

  def lift[M[_], T](body: Expr[T])(implicit m: WeakTypeTag[M[_]]): Tree = {
    val tree = Transformer[M](c)(body.tree)
    Trees.traverse(c)(tree) {
      case tree @ q"$pack.unlift[$t]($v)" =>
        c.error(tree.pos, "Invalid unlift position")
    }
//    c.info(c.enclosingPosition, tree.toString, false)
    tree
  }
}

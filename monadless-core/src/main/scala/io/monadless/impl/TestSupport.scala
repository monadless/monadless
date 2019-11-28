package io.monadless.impl

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context
import org.scalamacros.resetallattrs._
import scala.reflect.macros.TypecheckException

private[monadless] trait TestSupport[M[_]] {
  def get[T](m: M[T]): T
  def showTree[T](t: T): Unit = macro TestSupportMacro.showTree
  def showRawTree[T](t: T): Unit = macro TestSupportMacro.showRawTree
  def forceLift[T](t: T): T = macro TestSupportMacro.forceLift
  def runLiftTest[T](expected: T)(body: T): Unit = macro TestSupportMacro.runLiftTest[M, T]
}

private[monadless] class TestSupportMacro(val c: Context) {
  import c.universe._

  def showTree(t: Tree): Tree = {
    c.warning(c.enclosingPosition, t.toString)
    q"()"
  }
  def showRawTree(t: Tree): Tree = {
    c.warning(c.enclosingPosition, showRaw(t))
    q"()"
  }

  def forceLift(t: Tree): Tree =
    c.resetAllAttrs {
      Trees.Transform(c)(t) {
        case q"$pack.unlift[$t]($v)" =>
          q"${c.prefix}.get($v)"
      }
    }

  def runLiftTest[M[_], T](expected: Tree)(body: Tree): Tree =
    c.resetAllAttrs {

      val lifted =
        q"${c.prefix}.get(${c.prefix}.lift($body))"

      val forceLifted = forceLift(body)

      q"""
        val expected = scala.util.Try($expected)
        assert(expected == ${typecheckToTry(lifted, "lifted")})
        assert(expected == ${typecheckToTry(forceLifted, "force lifted")})
        ()
      """
    }

  def typecheckToTry(tree: Tree, name: String): Tree = {
    try {
      val typeCheckedTree = c.typecheck(c.resetAllAttrs(tree))
      c.info(c.enclosingPosition, s"$name: $typeCheckedTree", force = false)
      q"scala.util.Try($typeCheckedTree)"
    } catch {
      case e: TypecheckException =>
        val msg = s"""
          |$name fails typechecking: $e
          |tree: $tree
          |""".stripMargin
        c.info(e.pos.asInstanceOf[Position], msg, force = true)
        q"""scala.util.Failure(new Exception($msg))"""
    }
  }
}

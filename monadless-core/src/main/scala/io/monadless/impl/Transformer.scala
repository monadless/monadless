package io.monadless.impl

import scala.language.higherKinds
import scala.reflect.macros.blackbox.Context
import org.scalamacros.resetallattrs._

private[monadless] object Transformer {

  def apply[M[_]](c: Context)(tree: c.Tree)(implicit m: c.WeakTypeTag[M[_]]): c.Tree = {
    import c.universe._

    object Transform {

      def apply(tree: Tree): Tree =
        unapply(tree).getOrElse(tree)

      def unapply(tree: Tree): Option[Tree] =
        c.untypecheck(tree) match {

          case PureTree(tree)                    => None

          case Typed(tree, tpe)                  => unapply(tree)

          case q"{ ..$trees }" if trees.size > 1 => TransformBlock.unapply(trees)

          case q"if(${ Transform(monad) }) $ifTrue else $ifFalse" =>
            val name = freshName
            val body = q"if($name) $ifTrue else $ifFalse"
            Some(Nest(monad, name, body))

          case q"if($cond) $ifTrue else $ifFalse" =>
            (ifTrue, ifFalse) match {
              case (Transform(ifTrue), Transform(ifFalse)) =>
                Some(q"if($cond) $ifTrue else $ifFalse")
              case (Transform(ifTrue), ifFalse) =>
                Some(q"if($cond) $ifTrue else ${c.prefix}($ifFalse)")
              case (ifTrue, Transform(ifFalse)) =>
                Some(q"if($cond) ${c.prefix}($ifTrue) else $ifFalse")
              case (ifTrue, ifFalse) =>
                None
            }

          case q"${ Transform(monad) } match { case ..$cases }" =>
            val name = freshName
            val body = q"$name match { case ..$cases }"
            Some(Nest(monad, name, body))

          case q"$value match { case ..${ TransformCases(cases) } }" =>
            Some(q"$value match { case ..$cases }")

          case q"while($cond) $body" =>
            val name = TermName(c.freshName("while"))
            val newBody = Transform(q"if($cond) { $body; ${c.prefix}.unlift[scala.Unit]($name()) }")
            Some(q"{ def $name(): $unitMonadType = $newBody; $name() }")

          case q"do $body while($cond)" =>
            val name = TermName(c.freshName("doWhile"))
            val newBody = Transform(q"{ $body; if($cond) ${c.prefix}.unlift[scala.Unit]($name()) else ${c.prefix}(scala.Unit) }")
            Some(q"{ def $name(): $unitMonadType = $newBody; $name() }")

          case q"$a && $b" =>
            (a, b) match {
              case (Transform(a), Transform(b)) =>
                Some(q"$a.flatMap { case true => $b; case false => ${c.prefix}(false) }")
              case (Transform(a), b) =>
                Some(q"$a.map { _ && $b }")
              case (a, Transform(b)) =>
                Some(q"if($a) $b else ${c.prefix}(false)")
            }

          case q"$a || $b" =>
            (a, b) match {
              case (Transform(a), Transform(b)) =>
                Some(q"$a.flatMap { case true => ${c.prefix}(true); case false => $b }")
              case (Transform(a), b) =>
                Some(q"$a.map { _ || $b }")
              case (a, Transform(b)) =>
                Some(q"if($a) ${c.prefix}(true) else $b")
            }

          // TODO what if the monad creation throws before creating the instance?
          case q"try $tryBlock catch { case ..$cases } finally $finallyBlock" =>
            val monad =
              (tryBlock, cases) match {
                case (Transform(tryBlock), TransformCases(cases)) =>
                  q"${c.prefix}.rescue($tryBlock) { case ..$cases }"
                case (Transform(tryBlock), Nil) =>
                  q"$tryBlock"
                case (Transform(tryBlock), cases) =>
                  q"${c.prefix}.handle($tryBlock) { case ..$cases }"
                case (tryBlock, TransformCases(cases)) =>
                  q"${c.prefix}.rescue(${c.prefix}($tryBlock)) { case ..$cases }"
                case other =>
                  q"${c.prefix}(try $tryBlock catch { case ..$cases })"
              }

            finallyBlock match {
              case EmptyTree    => Some(monad)
              case finallyBlock => Some(q"${c.prefix}.ensure($monad)(${Transform(finallyBlock)})")
            }

          case q"$pack.unlift[$t]($v)" => Some(v)

          case tree =>
            var unlifts = List.empty[(Tree, TermName)]
            val newTree =
              Trees.Transform(c)(tree) {
                case q"$pack.unlift[$t]($v)" =>
                  val name = freshName
                  unlifts :+= ((v, name))
                  q"$name"
              }

            unlifts match {
              case List()             => None
              case List((tree, name)) => Some(q"$tree.map(${toVal(name)} => $newTree)")
              case unlifts =>
                val (trees, names) = unlifts.unzip
                val binds = names.map(name => pq"$name @ _")
                Some(q"${c.prefix}.join(..$trees).map { case (..$binds) => $newTree }")
            }
        }
    }

    object TransformDefs {

      def apply(tree: Tree): Tree = {
        var unlifted = Set[Symbol]()

        object UnliftDefs {
          def apply(tree: Tree): Tree =
            Trees.Transform(c)(tree) {

              case tree @ q"$mods def $method[..$t](...$params): $r = ${ PureTree(body) }" =>
                tree

              case tree @ q"$mods def $method[..$t](...$params): $r = $body" =>
                unlifted += tree.symbol
                q"$mods def $method[..$t](...$params): ${monadType(r.tpe)} = ${Transform(UnliftDefs(body))}"

              case tree @ q"$pack.unlift[$t]($v)" => tree

              case tree @ q"$method(..$params)" if unlifted.contains(method.symbol) =>
                tree match {
                  case Transform(_) =>
                    c.abort(c.enclosingPosition, "Can't unlift parameters of a method with unlifted body.")
                  case tree =>
                    q"${c.prefix}.unlift[${tree.tpe}]($tree)"
                }

              case tree if unlifted.contains(tree.symbol) =>
                q"${c.prefix}.unlift[${tree.tpe}]($tree)"
            }
        }

        UnliftDefs(tree) match {
          case `tree` => tree
          case tree   => UnliftDefs(tree)
        }
      }
    }

    object Nest {
      def apply(monad: Tree, name: TermName, body: Tree): Tree =
        body match {
          case Transform(body) => q"$monad.flatMap(${toVal(name)} => $body)"
          case body            => q"$monad.map(${toVal(name)} => $body)"
        }
    }

    object TransformBlock {
      def unapply(trees: List[Tree]): Option[Tree] =
        trees match {

          case q"$mods val $name: $t = ${ Transform(monad) }" :: tail =>
            Some(Nest(monad, name, q"{ ..$tail }"))

          case Transform(head) :: Nil =>
            Some(head)

          case Transform(monad) :: tail =>
            Some(Nest(monad, wildcard, q"{ ..$tail }"))

          case head :: TransformBlock(q"{ ..$tail }") =>
            Some(q"{ $head; ..$tail }")

          case other => None
        }
    }

    object PureTree {
      def unapply(tree: Tree): Option[Tree] =
        Trees.exists(c)(tree) {
          case q"$pack.unlift[$t]($v)" => true
        } match {
          case true  => None
          case false => Some(tree)
        }
    }

    object TransformCases {
      def unapply(cases: List[Tree]) =
        cases.exists {
          case cq"$pattern => ${ Transform(body) }" => true
          case cq"$pattern if $cond => ${ Transform(body) }" => true
          case _ => false
        } match {
          case true =>
            Some {
              cases.map {
                case cq"$pattern => ${ Transform(body) }"          => cq"$pattern => $body"
                case cq"$pattern => $body"                         => cq"$pattern => ${c.prefix}($body)"
                case cq"$pattern if $cond => ${ Transform(body) }" => cq"$pattern if $cond => $body"
                case cq"$pattern if $cond => $body"                => cq"$pattern if $cond => ${c.prefix}($body)"
              }
            }
          case false => None
        }
    }

    object Validate {

      def apply(tree: Tree) =
        Trees.traverse(c)(tree) {

          case q"$v match { case ..$cases }" =>
            cases.foreach {
              case cq"$pattern if ${ t @ Transform(_) } => $body" =>
                c.abort(t.pos, s"Unlift can't be used as a case guard.")
              case other => ()
            }

          case t @ q"return $v" =>
            c.abort(t.pos, "Lifted expression can't contain `return` statements.")

          case q"$mods val $name = ${ t @ Transform(_) }" if mods.hasFlag(Flag.LAZY) =>
            c.abort(t.pos, "Unlift can't be used as a lazy val initializer.")

          case q"$mods def $name = ${ t @ Transform(_) }" if mods.hasFlag(Flag.LAZY) =>
            c.abort(t.pos, "Unlift can't be used as a lazy val initializer.")

          case q"(..$params) => ${ t @ Transform(_) }" =>
            c.abort(t.pos, "Unlift can't be used in function bodies.")

          case tree @ q"$method[..$t](...$values)" if values.size > 0 =>
            val pit = method.symbol.asMethod.paramLists.flatten.iterator
            val vit = values.flatten.iterator
            while (pit.hasNext && vit.hasNext) {
              val param = pit.next()
              val value = vit.next()
              (param.asTerm.isByNameParam, value) match {
                case (true, t @ Transform(_)) =>
                  c.abort(t.pos, "Unlift can't be used as by-name param.")
                case other => ()
              }
            }

          case t @ q"$mods def $method[..$tpe](...$params) = ${ Transform(body) }" =>
            if (t.symbol.overrides.nonEmpty)
              c.abort(t.pos, "Can't unlift overriden method body.")
        }
    }

    def toVal(name: TermName) = q"val $name = $EmptyTree"
    def wildcard = TermName("_")
    def freshName = TermName(c.freshName("x"))
    def monadType(payload: Type) =
      m.tpe.erasure.map { t =>
        if (t == c.typeOf[Any]) payload
        else t
      }
    def unitMonadType = monadType(c.typeOf[Unit])
    def debug[T](v: T) = {
      c.warning(c.enclosingPosition, v.toString)
      v
    }

    Validate(tree)

    c.resetAllAttrs {
      TransformDefs(tree) match {
        case PureTree(tree) => q"${c.prefix}($tree)"
        case tree           => Transform(tree)
      }
    }
  }
}

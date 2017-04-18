package io.monadless.impl

import language.higherKinds
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
            val name = freshName()
            val body = q"if($name) $ifTrue else $ifFalse"
            Some(Nest(monad, name, body))

          case q"if($cond) $ifTrue else $ifFalse" =>
            (ifTrue, ifFalse) match {
              case (Transform(ifTrue), Transform(ifFalse)) =>
                Some(q"if($cond) $ifTrue else $ifFalse")
              case (Transform(ifTrue), ifFalse) =>
                Some(q"if($cond) $ifTrue else ${Resolve.apply(tree.pos)}($ifFalse)")
              case (ifTrue, Transform(ifFalse)) =>
                Some(q"if($cond) ${Resolve.apply(tree.pos)}($ifTrue) else $ifFalse")
              case (ifTrue, ifFalse) =>
                None
            }

          case q"${ Transform(monad) } match { case ..$cases }" =>
            val name = freshName()
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
            val newBody = Transform(q"{ $body; if($cond) ${c.prefix}.unlift[scala.Unit]($name()) else ${Resolve.apply(tree.pos)}(scala.Unit) }")
            Some(q"{ def $name(): $unitMonadType = $newBody; $name() }")

          case q"$a && $b" =>
            (a, b) match {
              case (Transform(a), Transform(b)) =>
                Some(q"${Resolve.flatMap(tree.pos, a)} { case true => $b; case false => ${Resolve.apply(tree.pos)}(false) }")
              case (Transform(a), b) =>
                Some(q"${Resolve.map(tree.pos, a)} { _ && $b }")
              case (a, Transform(b)) =>
                Some(q"if($a) $b else ${Resolve.apply(tree.pos)}(false)")
            }

          case q"$a || $b" =>
            (a, b) match {
              case (Transform(a), Transform(b)) =>
                Some(q"${Resolve.flatMap(tree.pos, a)} { case true => ${Resolve.apply(tree.pos)}(true); case false => $b }")
              case (Transform(a), b) =>
                Some(q"${Resolve.map(tree.pos, a)} { _ || $b }")
              case (a, Transform(b)) =>
                Some(q"if($a) ${Resolve.apply(tree.pos)}(true) else $b")
            }

          // TODO what if the monad creation throws before creating the instance?
          case q"try $tryBlock catch { case ..$cases } finally $finallyBlock" =>
            val monad =
              (tryBlock, cases) match {
                case (Transform(tryBlock), TransformCases(cases)) =>
                  q"${Resolve.rescue(tree.pos, tryBlock)} { case ..$cases }"
                case (Transform(tryBlock), Nil) =>
                  q"$tryBlock"
                case (Transform(tryBlock), cases) =>
                  q"${Resolve.rescue(tree.pos, tryBlock)} { case ..${TransformCases(cases)} }"
                case (tryBlock, TransformCases(cases)) =>
                  q"${Resolve.rescue(tree.pos, q"${c.prefix}($tryBlock)")} { case ..$cases }"
                case other =>
                  q"${Resolve.apply(tree.pos)}(try $tryBlock catch { case ..$cases })"
              }

            finallyBlock match {
              case EmptyTree    => Some(monad)
              case finallyBlock => Some(q"${Resolve.ensure(tree.pos, monad)}(${Transform(finallyBlock)})")
            }

          case q"$pack.unlift[$t]($v)" => Some(v)

          case tree =>
            var unlifts = List.empty[(Tree, TermName, Type)]
            val newTree =
              Trees.Transform(c)(tree) {
                case q"$pack.unlift[${ t: Type }]($v)" =>
                  val name = freshName()
                  unlifts :+= ((v, name, t))
                  q"$name"
              }

            unlifts match {
              case List()                  => None
              case List((tree, name, tpe)) => Some(q"${Resolve.map(tree.pos, tree)}(${toVal(name)} => $newTree)")
              case unlifts =>
                val (trees, names, types) = unlifts.unzip3
                val binds = names.map(name => pq"$name @ _")
                val list = freshName("list")
                val iterator = freshName("iterator")
                val collect = q"${Resolve.collect(tree.pos)}(scala.List(..$trees))"
                Some(
                  q"""
                    ${Resolve.map(tree.pos, collect)} { ${toVal(list)} =>
                      val $iterator = $list.iterator
                      ..${unlifts.map { case (tree, name, tpe) => q"val $name = $iterator.next().asInstanceOf[$tpe]" }}
                      $newTree
                    }
                  """
                )
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
          case Transform(body) => q"${Resolve.flatMap(monad.pos, monad)}(${toVal(name)} => $body)"
          case body            => q"${Resolve.map(monad.pos, monad)}(${toVal(name)} => $body)"
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

      def apply(cases: List[Tree]) =
        cases.map {
          case t @ cq"$pattern => ${ Transform(body) }"          => cq"$pattern => $body"
          case t @ cq"$pattern => $body"                         => cq"$pattern => ${Resolve.apply(t.pos)}($body)"
          case t @ cq"$pattern if $cond => ${ Transform(body) }" => cq"$pattern if $cond => $body"
          case t @ cq"$pattern if $cond => $body"                => cq"$pattern if $cond => ${Resolve.apply(t.pos)}($body)"
        }

      def unapply(cases: List[Tree]) =
        cases.exists {
          case cq"$pattern => ${ Transform(body) }" => true
          case cq"$pattern if $cond => ${ Transform(body) }" => true
          case _ => false
        } match {
          case true  => Some(apply(cases))
          case false => None
        }
    }

    object Resolve {

      private val monadTypeName = m.tpe.typeSymbol.name.decodedName
      private val sourceCompatibilityMessage =
        s"""For instance, it's possible to add implicits or default parameters to the method 
            |without breaking source compatibility.
            |Note: the methods defined by the `Monadless` instance have precedence over the ones
            |defined by the monad instance and its companion object.
        """.stripMargin

      def apply(pos: Position): Tree =
        companionMethod(pos, "apply").getOrElse {
          val msg =
            s"""Transformation requires the method `apply` to create a monad instance for a value.
               |${companionMethodErrorMessage(s"def apply[T](v: => T): $monadTypeName[T]")}
            """.stripMargin
          c.abort(pos, msg)
        }

      def collect(pos: Position): Tree =
        companionMethod(pos, "collect").getOrElse {
          val msg =
            s"""Transformation requires the method `collect` to transform List[M[T]] into M[List[T]]. The implementation
               |is free to collect the results sequentially or in parallel.
               |${companionMethodErrorMessage(s"def collect[T](list: List[$monadTypeName[T]]): $monadTypeName[List[T]]")}
            """.stripMargin
          c.abort(pos, msg)
        }

      private def companionMethodErrorMessage(signature: String) =
        s"""Please add the method to `${m.tpe}`'s companion object or to `${c.prefix.tree}`.
           |It needs to be source compatible with the following signature:
           |`$signature`
           |$sourceCompatibilityMessage
        """.stripMargin

      def map(pos: Position, instance: Tree): Tree =
        instanceMethod(pos, instance, "map").getOrElse {
          val msg =
            s"""Transformation requires the method `map` to transform the result of a monad instance.
               |${instanceMethodErrorMessage("map[T, U]", s"f: T => U", s"$monadTypeName[U]")}
            """.stripMargin
          c.abort(pos, msg)
        }

      def flatMap(pos: Position, instance: Tree): Tree =
        instanceMethod(pos, instance, "flatMap").getOrElse {
          val msg =
            s"""Transformation requires the method `flatMap` to transform the result of a monad instance.
               |${instanceMethodErrorMessage("flatMap[T, U]", s"f: T => $monadTypeName[U]", s"$monadTypeName[U]")}
            """.stripMargin
          c.abort(pos, msg)
        }

      def rescue(pos: Position, instance: Tree): Tree =
        instanceMethod(pos, instance, "rescue").getOrElse {
          val msg =
            s"""Transformation requires the method `rescue` to recover from a failure (translate a `catch` clause).
               |${instanceMethodErrorMessage("collect[T]", s"pf: PartialFunction[Throwable, $monadTypeName[T]]", s"$monadTypeName[T]")}
               |$errorHandlingMonadNote
            """.stripMargin
          c.abort(pos, msg)
        }

      def ensure(pos: Position, instance: Tree): Tree =
        instanceMethod(pos, instance, "ensure").getOrElse {
          val msg =
            s"""Transformation requires the method `ensure` to execute code regardless of the outcome of the 
               |execution (translate a `finally` clause).
               |${instanceMethodErrorMessage("rescue[T]", s"f: => Unit", s"$monadTypeName[T]")}
               |$errorHandlingMonadNote
            """.stripMargin
          c.abort(pos, msg)
        }

      private def instanceMethodErrorMessage(name: String, parameter: String, result: String) =
        s"""Please add the method to `${m.tpe}` or to `${c.prefix.tree}`.
           |It needs to be source compatible with the following signature:
           |As a `${m.tpe}` method: `def $name($parameter): $result`
           |As a `${c.prefix.tree}` method: `def $name[T](m: $monadTypeName[T])($parameter): $result`
           |$sourceCompatibilityMessage
        """.stripMargin

      private val errorHandlingMonadNote =
        """Note that this kind of construct (`try`/`catch`/`finally`) can't be used with monads 
          |that don't represent a computation and/or don't handle exceptions (e.g. `Option`)
        """.stripMargin

      private def instanceMethod(pos: Position, instance: Tree, name: String) =
        this.method(c.prefix.tree, c.prefix.tree.tpe, TermName(name)).map(t => q"$t($instance)")
          .orElse(this.method(instance, m.tpe, TermName(name)))

      private def companionMethod(pos: Position, name: String) =
        method(c.prefix.tree, c.prefix.tree.tpe, TermName(name))
          .orElse(method(q"${m.tpe.typeSymbol.companion}", m.tpe.companion, TermName(name)))

      private def method(instance: Tree, tpe: Type, name: TermName) =
        find(tpe, name).map(_ => q"$instance.$name")

      private def find(tpe: Type, method: TermName) =
        tpe.member(method) match {
          case NoSymbol => None
          case symbol   => Some(symbol)
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
    def freshName(x: String = "x") = TermName(c.freshName(x))
    def monadType(tpe: Type) = tq"${c.prefix}.M[$tpe]"
    def unitMonadType = monadType(c.typeOf[Unit])
    def debug[T](v: T) = {
      c.warning(c.enclosingPosition, v.toString)
      v
    }

    Validate(tree)

    c.resetAllAttrs {
      TransformDefs(tree) match {
        case PureTree(tree) => q"${Resolve.apply(tree.pos)}($tree)"
        case tree           => Transform(tree)
      }
    }
  }
}

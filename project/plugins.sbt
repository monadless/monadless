resolvers += Classpaths.sbtPluginReleases

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("org.tpolecat" % "tut-plugin" % "0.6.10")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "5.2.4")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.11")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.1")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "2.5")

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.1.14")

addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.8.2")

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.26")

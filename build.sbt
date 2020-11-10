import ReleaseTransformations._
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._
import sbtrelease.ReleasePlugin

enablePlugins(TutPlugin)

lazy val superPure = new org.scalajs.sbtplugin.cross.CrossType {
  def projectDir(crossBase: File, projectType: String): File =
    projectType match {
      case "jvm" => crossBase
      case "js"  => crossBase / s".$projectType"
    }

  def sharedSrcDir(projectBase: File, conf: String): Option[File] =
    Some(projectBase.getParentFile / "src" / conf / "scala")
}

lazy val `monadless` =
  (project in file("."))
    .settings(commonSettings)
    .aggregate(
      `monadless-core-jvm`, `monadless-core-js`, 
      `monadless-lst-jvm`, //`monadless-lst-js`,
      `monadless-stdlib-jvm`, `monadless-stdlib-js`,
      `monadless-cats-jvm`, `monadless-cats-js`, 
      `monadless-monix-jvm`, `monadless-monix-js`, 
      `monadless-algebird`, `monadless-examples`
    )
    .dependsOn(
      `monadless-core-jvm`, `monadless-core-js`,
      `monadless-lst-jvm`, //`monadless-lst-js`,
      `monadless-stdlib-jvm`, `monadless-stdlib-js`,
      `monadless-cats-jvm`, `monadless-cats-js`, 
      `monadless-monix-jvm`, `monadless-monix-js`,
      `monadless-algebird`
    )

lazy val `monadless-lst` = 
  crossProject.crossType(superPure)
    .settings(commonSettings)
    .settings(
      name := "monadless-lst",
      libraryDependencies ++= Seq(
        "org.typelevel" %% "discipline-scalatest" % "1.0.0-RC1" % Test,
        "org.typelevel" %% "cats-testkit" % "2.0.0" % "test",
        "org.scalatest" %%% "scalatest" % "3.0.8" % "test"),
      scoverage.ScoverageKeys.coverageMinimum := 96,
      scoverage.ScoverageKeys.coverageFailOnMinimum := false)
    .jsSettings(
      coverageExcludedPackages := ".*"
    )

lazy val `monadless-lst-jvm` = `monadless-lst`.jvm
// lazy val `monadless-lst-js` = `monadless-lst`.js

lazy val `monadless-core` = 
  crossProject.crossType(superPure)
    .settings(commonSettings)
    .settings(
      name := "monadless-core",
      libraryDependencies ++= Seq(
        "org.scalamacros" %% "resetallattrs" % "1.0.0",
        "org.scala-lang" % "scala-reflect" % scalaVersion.value,
        "org.scalatest" %%% "scalatest" % "3.0.8" % "test"),
      scoverage.ScoverageKeys.coverageMinimum := 96,
      scoverage.ScoverageKeys.coverageFailOnMinimum := false)
    .jsSettings(
      coverageExcludedPackages := ".*"
    )

lazy val `monadless-core-jvm` = `monadless-core`.jvm
lazy val `monadless-core-js` = `monadless-core`.js

lazy val `monadless-stdlib` = 
  crossProject.crossType(superPure)
    .dependsOn(`monadless-core`)
    .settings(commonSettings)
    .settings(
      name := "monadless-stdlib",
      libraryDependencies += "org.scalatest" %%% "scalatest" % "3.0.8" % "test",
      scoverage.ScoverageKeys.coverageMinimum := 96,
      scoverage.ScoverageKeys.coverageFailOnMinimum := false)
    .jsSettings(
      coverageExcludedPackages := ".*"
    )

lazy val `monadless-stdlib-jvm` = `monadless-stdlib`.jvm
lazy val `monadless-stdlib-js` = `monadless-stdlib`.js


lazy val `monadless-cats` = 
  crossProject.crossType(superPure)
    .dependsOn(`monadless-core`)
    .settings(commonSettings)
    .settings(
      name := "monadless-cats",
      libraryDependencies ++= Seq(
        "org.scalatest" %%% "scalatest" % "3.0.8" % "test",
        "org.typelevel" %%% "cats-core" % "2.0.0"
      ),
      scoverage.ScoverageKeys.coverageMinimum := 96,
      scoverage.ScoverageKeys.coverageFailOnMinimum := false)
    .jsSettings(
      coverageExcludedPackages := ".*"
    )

lazy val `monadless-cats-jvm` = `monadless-cats`.jvm
lazy val `monadless-cats-js` = `monadless-cats`.js


lazy val `monadless-monix` = 
  crossProject.crossType(superPure)
    .dependsOn(`monadless-core`)
    .settings(commonSettings)
    .settings(
      name := "monadless-monix",
      libraryDependencies ++= Seq(
        "org.scalatest" %%% "scalatest" % "3.0.8" % "test",
        "io.monix" %%% "monix" % "3.1.0"
      ),
      scoverage.ScoverageKeys.coverageMinimum := 96,
      scoverage.ScoverageKeys.coverageFailOnMinimum := false)
    .jsSettings(
      coverageExcludedPackages := ".*"
    )

lazy val `monadless-monix-jvm` = `monadless-monix`.jvm
lazy val `monadless-monix-js` = `monadless-monix`.js

lazy val `monadless-algebird` = project
  .dependsOn(`monadless-core-jvm`)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.twitter" %% "algebird-core" % "0.13.6",
      "org.scalatest" %%% "scalatest" % "3.0.8" % "test"
    )
  )

lazy val `monadless-examples` = project
  .dependsOn(`monadless-stdlib-jvm`)
  .settings(commonSettings)
  .settings(
    libraryDependencies += "org.scala-lang.modules" %% "scala-java8-compat" % "0.9.0",
    libraryDependencies += "com.typesafe.play" %% "play-ahc-ws" % "2.7.3",
    libraryDependencies += "org.scala-lang.modules" %% "scala-async" % "0.10.0")

def updateReadmeVersion(selectVersion: sbtrelease.Versions => String) =
  ReleaseStep(action = st => {

    val newVersion = selectVersion(st.get(ReleaseKeys.versions).get)

    import scala.io.Source
    import java.io.PrintWriter

    val pattern = """"io.monadless" %% "monadless-.*" % "(.*)"""".r

    val fileName = "README.md"
    val content = Source.fromFile(fileName).getLines.mkString("\n")

    val newContent =
      pattern.replaceAllIn(content,
        m => m.matched.replaceAllLiterally(m.subgroups.head, newVersion))

    new PrintWriter(fileName) { write(newContent); close }

    val vcs = Project.extract(st).get(releaseVcs).get
    vcs.add(fileName).!

    st
  })

def updateWebsiteTag =
  ReleaseStep(action = st => {

    val vcs = Project.extract(st).get(releaseVcs).get
    vcs.tag("website", "update website", false).!

    st
  })

lazy val commonSettings = Seq(
  scalaVersion := "2.13.1",
  crossScalaVersions := Seq("2.13.1","2.12.8"),
  organization := "io.monadless",
  EclipseKeys.eclipseOutput := Some("bin"),
  scalacOptions ++= Seq(
    "-Xfatal-warnings",
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-unchecked",
    "-Xlint",
    "-Ywarn-numeric-widen") ++ (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2,13)) => Seq()
    case _ => Seq("-language:higherKinds","-Yno-adapted-args","-Xfuture","-Ywarn-unused-import")
  }),
  ScalariformKeys.preferences := ScalariformKeys.preferences.value
    .setPreference(AlignParameters, true)
    .setPreference(CompactStringConcatenation, false)
    .setPreference(IndentPackageBlocks, true)
    .setPreference(FormatXml, true)
    .setPreference(PreserveSpaceBeforeArguments, false)
    .setPreference(DoubleIndentClassDeclaration, false)
    .setPreference(RewriteArrowSymbols, false)
    .setPreference(AlignSingleLineCaseStatements, true)
    .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 40)
    .setPreference(SpaceBeforeColon, false)
    .setPreference(SpaceInsideBrackets, false)
    .setPreference(SpaceInsideParentheses, false)
    .setPreference(DanglingCloseParenthesis, Force)
    .setPreference(IndentSpaces, 2)
    .setPreference(IndentLocalDefs, false)
    .setPreference(SpacesWithinPatternBinders, true)
    .setPreference(SpacesAroundMultiImports, true),
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  releaseIgnoreUntrackedFiles := true,
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  pgpSecretRing := file("local.secring.gpg"),
  pgpPublicRing := file("local.pubring.gpg"),
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    updateReadmeVersion(_._1),
    commitReleaseVersion,
    updateWebsiteTag,
    tagRelease,
    publishArtifacts,
    setNextVersion,
    updateReadmeVersion(_._2),
    commitNextVersion,
    ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
    pushChanges
),
  pomExtra := (
    <url>http://github.com/monadless/monadless</url>
    <licenses>
      <license>
        <name>Apache License 2.0</name>
        <url>https://raw.githubusercontent.com/monadless/monadless/master/LICENSE.txt</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:monadless/monadless.git</url>
      <connection>scm:git:git@github.com:monadless/monadless.git</connection>
    </scm>
    <developers>
      <developer>
        <id>fwbrasil</id>
        <name>Flavio W. Brasil</name>
        <url>http://github.com/fwbrasil/</url>
      </developer>
      <developer>
        <id>sameerparekh</id>
        <name>Sameer Parekh</name>
        <url>http://github.com/sameerparekh/</url>
      </developer>
    </developers>)
)

lazy val `tut-sources` = Seq(
  "README.md"
)

lazy val `tut-settings` = Seq(
  scalacOptions := Seq(),
  tutSourceDirectory := baseDirectory.value / "target" / "tut",
  tutNameFilter := `tut-sources`.map(_.replaceAll("""\.""", """\.""")).mkString("(", "|", ")").r,
  sourceGenerators in Compile +=
    Def.task {
      `tut-sources`.foreach { name =>
        val source = baseDirectory.value / name
        val file = baseDirectory.value / "target" / "tut" / name
        val str = IO.read(source).replace("```scala", "```tut")
        IO.write(file, str)
      }
      Seq()
    }.taskValue
)

commands += Command.command("checkUnformattedFiles") { st =>
  val vcs = Project.extract(st).get(releaseVcs).get
  val modified = vcs.cmd("ls-files", "--modified", "--exclude-standard").!!.trim
  if(modified.nonEmpty)
    throw new IllegalStateException(s"Please run `sbt scalariformFormat test:scalariformFormat` and resubmit your pull request. Found unformatted files: \n$modified")
  st
}

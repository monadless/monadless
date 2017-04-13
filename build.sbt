import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._

lazy val commonSettings = Seq(
  scalaVersion := "2.11.8",
  organization := "io.monadless",
  EclipseKeys.eclipseOutput := Some("bin"),
  scalacOptions ++= Seq(
    "-Xfatal-warnings",
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-unchecked",
    "-Xlint",
    "-Yno-adapted-args",
    "-Ywarn-numeric-widen",
    "-Xfuture",
    "-Ywarn-unused-import"),
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
    .setPreference(SpacesAroundMultiImports, true))

lazy val `monadless-core` = project
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalamacros" %% "resetallattrs" % "1.0.0",
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.scalatest" % "scalatest_2.11" % "3.0.1" % "test"),
    scoverage.ScoverageKeys.coverageMinimum := 96,
    scoverage.ScoverageKeys.coverageFailOnMinimum := false)

// lazy val `monadless-examples` = project
//   .dependsOn(`monadless-core`)
//   .settings(commonSettings: _*)
//   .settings(
//     libraryDependencies += "com.typesafe.play" %% "play-ws" % "2.4.3",
//     libraryDependencies += "org.scala-lang.modules" %% "scala-async" % "0.9.6")
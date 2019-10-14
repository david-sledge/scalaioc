ThisBuild / organization := "io.github.david-sledge"
ThisBuild / scalaVersion := "2.12.10"
//ThisBuild / version := "1.0.0-alpha.3"

scalacOptions ++= Seq(
  "-deprecation", "-feature",
)

import xerial.sbt.Sonatype._

lazy val root = (project in file("."))
  .settings(
    unmanagedClasspath in Runtime += baseDirectory.value / "src/main/resources",
    name := "scalaioc",
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % "2.12.10",
      "org.scala-lang" % "scala-reflect" % "2.12.10",
      "org.scalatest" %% "scalatest" % "3.0.4" % "test",
      "javax.servlet" % "javax.servlet-api" % "3.0.1" % "provided",
    ),
    mainClass in (Compile, packageBin) := Some("scala.ioc.cli.Main"),
  )

publishTo := sonatypePublishTo.value
//publishConfiguration := publishConfiguration.value.withOverwrite(true)
//publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)

ThisBuild / organization := "io.github.david-sledge"
ThisBuild / scalaVersion := "2.12.8"
ThisBuild / version := "v1.0.0-alpha.2-SNAPSHOT"

scalacOptions ++= Seq(
  "-deprecation", "-feature",
)

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-compiler" % "2.12.8",
  "org.scala-lang" % "scala-reflect" % "2.12.8",
  "org.scalatest" %% "scalatest" % "3.0.4" % "test",
)

import xerial.sbt.Sonatype._

lazy val scalaioc = (project in file("."))
  .settings(
    unmanagedClasspath in Runtime += baseDirectory.value / "src/main/resources",
    name := "scalaioc",
    libraryDependencies += "javax.servlet" % "javax.servlet-api" % "3.0.1" %
      "provided",
    description := "An IoC/DI framework written in scala",
  )

publishTo := sonatypePublishTo.value

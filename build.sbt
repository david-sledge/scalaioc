
lazy val commonSettings = Seq(
    organization := "io.github.david-sledge",
    scalaVersion := "2.12.8",
	licenses := Seq("GPL3" -> url("https://www.gnu.org/licenses/gpl-3.0.en.html"))
  )

scalacOptions ++= Seq(
  "-deprecation", "-feature"
)

libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-compiler" % "2.12.8",
  "org.scala-lang" % "scala-reflect" % "2.12.8",
  "org.scalatest" %% "scalatest" % "3.0.4" % "test"
)

import xerial.sbt.Sonatype._

lazy val root = (project in file("."))
  .settings(
    commonSettings,
    unmanagedClasspath in Runtime += baseDirectory.value / "src/main/resources",
    name := "scalaioc",
    libraryDependencies += "javax.servlet" % "javax.servlet-api" % "3.0.1" %
      "provided",
    description := "An IoC/DI framework written in scala",
    sonatypeProjectHosting := Some(GitHubHosting("david-sledge", "scalaioc", "sledged@gmail.com")),
    publishTo := sonatypePublishTo.value
  )

lazy val simpleExample = (project in file("examples/simple"))
  .settings(
    commonSettings,
    unmanagedClasspath in Runtime += baseDirectory.value / "src/main/resources",
    name := "simpleExample"
  ).dependsOn(root)

lazy val servletExample = (project in file("examples/servlet"))
  .settings(
    commonSettings,
    name := "servletExample",
    libraryDependencies += "javax.servlet" % "javax.servlet-api" % "3.0.1" %
      "provided"
  ).enablePlugins(TomcatPlugin).dependsOn(root)

lazy val cliExample = (project in file("examples/cli"))
  .settings(
    commonSettings,
    name := "cliExample",
    libraryDependencies += "net.sf.jopt-simple" % "jopt-simple" % "5.0.4"
  ).dependsOn(root)

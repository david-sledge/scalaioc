
lazy val commonSettings = Seq(
    organization := "io.github.david-sledge",
    scalaVersion := "2.12.10",
	licenses := Seq("GPL3" -> url("https://www.gnu.org/licenses/gpl-3.0.en.html")),
  )

scalacOptions ++= Seq(
  "-deprecation", "-feature",
)

libraryDependencies ++= Seq(
  "io.github.david-sledge" % "scalaioc_2.12" % "1.0.0-alpha.2",
)

lazy val root = (project in file("."))
  .settings(
    commonSettings,
    name := "scalaioc-servlet-example",
    description := "A servlet example of the scalaioc framework",
  ).enablePlugins(TomcatPlugin)

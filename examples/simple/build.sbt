
lazy val commonSettings = Seq(
    organization := "io.github.david-sledge",
    scalaVersion := "2.12.8",
	licenses := Seq("GPL3" -> url("https://www.gnu.org/licenses/gpl-3.0.en.html")),
  )

scalacOptions ++= Seq(
  "-deprecation", "-feature",
)

libraryDependencies ++= Seq(
  "io.github.david-sledge" % "scalaioc_2.12" % "v1.0.0-alpha.1",
)

lazy val root = (project in file("."))
  .settings(
    commonSettings,
    unmanagedClasspath in Runtime += baseDirectory.value / "src/main/resources",
    name := "scalaioc-simple-example",
    description := "An simple example of the scalaioc framework",
  )

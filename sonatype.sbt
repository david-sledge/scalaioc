// To sync with Maven central, you need to supply the following information:
//publishMavenStyle := true

// License of your choice
licenses := Seq("GPL3" -> url("https://www.gnu.org/licenses/gpl-3.0.en.html"))

// Where is the source code hosted
import xerial.sbt.Sonatype._

sonatypeProjectHosting := Some(GitHubHosting("david-sledge", "scalaioc", "sledged@gmail.com"))

developers := List(
  Developer(id="david-sledge", name="David M. Sledge", email="sledged@gmail.com", url=url("https://github.com/david-sledge/"))
)

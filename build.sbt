lazy val baseName  = "Mellite-tutorials"
lazy val baseNameL = baseName.toLowerCase

// ---- dependencies ----

lazy val deps = new {
  val fscape  = "3.15.0"
  val mellite = "3.13.0"  // app
}

// ---- common ----

lazy val commonSettings = Seq(
  organization        := "de.sciss",
  homepage            := Some(url(s"https://git.iem.at/sciss/$baseName")),
  licenses            := Seq("GNU Affero General Public License v3+" -> url("http://www.gnu.org/licenses/agpl-3.0.txt")),
  description         := "Tutorials for the computer music environment Mellite",
  version             := "0.1.0-SNAPSHOT",
  scalaVersion        := "2.13.7",
  crossScalaVersions  := Seq("2.13.7"), // , "2.12.10"),  // N.B. nsc API has breakage in minor versions (2.13.0 Dotterweide fails on 2.13.1)
  scalacOptions      ++= Seq(
    "-deprecation", "-unchecked", "-feature", "-encoding", "utf8", "-Xlint:-stars-align,_", "-Xsource:2.13"
  ),
  scalacOptions in (Compile, compile) ++= (if (scala.util.Properties.isJavaAtLeast("9")) Seq("-release", "8") else Nil), // JDK >8 breaks API; skip scala-doc
  scalacOptions += "-Yrangepos",  // this is needed to extract source code
)

// ---- projects ----

lazy val root = project.withId(baseNameL).in(file("."))
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "de.sciss" %% "mellite-app"   % deps.mellite,
      "de.sciss" %% "fscape-macros" % deps.fscape,
    ),
  )

// Determine OS version of JavaFX binaries
lazy val jfxClassifier = sys.props("os.name") match {
  case n if n.startsWith("Linux")   => "linux"
  case n if n.startsWith("Mac")     => "mac"
  case n if n.startsWith("Windows") => "win"
  case _ => throw new Exception("Unknown platform!")
}

def jfxDep(name: String): ModuleID =
  "org.openjfx" % s"javafx-$name" % "11.0.2" classifier jfxClassifier

def archSuffix: String =
  sys.props("os.arch") match {
    case "i386"  | "x86_32" => "x32"
    case "amd64" | "x86_64" => "x64"
    case other              => other
  }


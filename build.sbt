lazy val baseName         = "Data-to-Process"
lazy val baseNameL        = baseName.toLowerCase
lazy val projectVersion   = "0.1.0-SNAPSHOT"

lazy val commonSettings = Seq(
  version             := projectVersion,
  organization        := "de.sciss",
  description         := "An algorithmic art project",
  homepage            := Some(url(s"https://github.com/Sciss/$baseName")),
  scalaVersion        := "2.12.3",
  licenses            := Seq(gpl2),
  scalacOptions      ++= Seq("-deprecation", "-unchecked", "-feature", "-encoding", "utf8", "-Xfuture", "-Xlint"),
  libraryDependencies ++= Seq(
    "de.sciss"          %% "fileutil"           % "1.1.3",
    "de.sciss"          %% "numbers"            % "0.1.3",
    "de.sciss"          %% "kollflitz"          % "0.2.1",
    "com.github.scopt"  %% "scopt"              % "3.7.0",
    "de.sciss"          %% "fscape"             % "2.9.0",
    "de.sciss"          %  "prefuse-core"       % "1.0.1",
    "de.sciss"          %% "play-json-sealed"   % "0.4.1",
    "de.sciss"          %% "desktop"            % "0.8.0",
    "de.sciss"          %% "guiflitz"           % "0.5.1",
    "com.mortennobel"   %  "java-image-scaling" % "0.8.6"   // includes jh filters
  )
)

lazy val gpl2 = "GPL v2+" -> url("http://www.gnu.org/licenses/gpl-2.0.txt")

lazy val root = Project(id = baseNameL, base = file("."))
  .settings(commonSettings)

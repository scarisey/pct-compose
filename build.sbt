import sbtassembly.AssemblyPlugin.defaultShellScript
organization := "dev.carisey"
homepage := Some(url("https://pct-compose.carisey.dev"))
licenses += "AGPL-3.0-or-later" -> url("https://www.gnu.org/licenses/agpl-3.0.fr.html#license-text")
developers := List(
  Developer("scarisey", "Sylvain Carisey", "sylvain@carisey.dev", url("https://github.com/scarisey"))
)
startYear := Some(2023)
scmInfo := Some(
  ScmInfo(
    url("https://github.com/scarisey/pct-compose"),
    "scm:git:git@github.com/scarisey/pct-compose.git"
  )
)

scalaVersion := "3.3.1"
testFrameworks += new TestFramework("org.scalatest.tools.Framework")
enablePlugins(NativeImagePlugin)

compile / mainClass := Some("dev.carisey.pctcompose.PctCompose")

nativeImageOptions += s"-H:ReflectionConfigurationFiles=${target.value / "native-image-configs" / "reflect-config.json"}"
nativeImageOptions += s"-H:ConfigurationFileDirectories=${target.value / "native-image-configs"}"
nativeImageOptions += "-H:+JNI"

assemblyPrependShellScript := Some(defaultShellScript)
assembly / assemblyJarName := "pct-compose"

libraryDependencies ++= Seq(
  "com.lihaoyi" %% "mainargs" % "0.5.4",
  "com.lihaoyi" %% "os-lib" % "0.9.2",
  "com.lihaoyi" %% "pprint" % "0.8.1",
  "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % "2.25.0",
  "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "2.25.0",
  "io.github.iltotore" %% "iron" % "2.3.0",
  "io.github.iltotore" %% "iron-jsoniter" % "2.3.0",
  "com.lihaoyi" %% "requests" % "0.8.0"
)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.17" % Test,
  "org.scalacheck" %% "scalacheck" % "1.17.0" % Test,
  "com.lihaoyi" %% "mainargs" % "0.5.4" % Test,
  "com.lihaoyi" %% "os-lib" % "0.9.2" % Test,
  "com.lihaoyi" %% "pprint" % "0.8.1" % Test,
  "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % "2.25.0" % Test,
  "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "2.25.0" % Test,
  "io.github.iltotore" %% "iron" % "2.3.0" % Test,
  "io.github.iltotore" %% "iron-scalacheck" % "2.3.0" % Test,
  "io.github.iltotore" %% "iron-jsoniter" % "2.3.0" % Test,
  "com.lihaoyi" %% "requests" % "0.8.0" % Test
)

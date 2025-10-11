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

scalaVersion := "3.7.3"
testFrameworks += new TestFramework("org.scalatest.tools.Framework")
enablePlugins(NativeImagePlugin)

compile / mainClass := Some("dev.carisey.pctcompose.PctCompose")

nativeImageOptions += s"-H:ReflectionConfigurationFiles=${baseDirectory.value / "build-native" / "native-image-configs" / "reflect-config.json"}"
nativeImageOptions += s"-H:ConfigurationFileDirectories=${baseDirectory.value / "build-native" / "native-image-configs"}"
nativeImageOptions += "-H:+JNI"
nativeImageOptions += "--no-fallback"
nativeImageOptions += "--enable-url-protocols=https"
nativeImageVersion := "21.0.1"
nativeImageJvm := "graalvm-community"

assemblyPrependShellScript := Some(defaultShellScript)
assembly / assemblyJarName := "pct-compose"

libraryDependencies ++= Seq(
  "com.lihaoyi" %% "mainargs" % "0.7.6",
  "com.lihaoyi" %% "os-lib" % "0.11.5",
  "com.lihaoyi" %% "pprint" % "0.9.4",
  "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % "2.38.3",
  "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "2.38.2",
  "io.github.iltotore" %% "iron" % "3.2.0",
  "io.github.iltotore" %% "iron-jsoniter" % "3.2.0",
  "com.lihaoyi" %% "requests" % "0.9.0",
  "com.lihaoyi" %% "fastparse" % "3.1.1",
  "com.softwaremill.magnolia1_3" %% "magnolia" % "1.3.18"
) ++ List("cats-core", "cats-kernel").map(dep => "org.typelevel" %% dep % "2.10.0")

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.19" % Test,
  "org.scalacheck" %% "scalacheck" % "1.19.0" % Test,
  "com.lihaoyi" %% "mainargs" % "0.7.6" % Test,
  "com.lihaoyi" %% "os-lib" % "0.11.5" % Test,
  "com.lihaoyi" %% "pprint" % "0.9.4" % Test,
  "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % "2.38.3" % Test,
  "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "2.38.2" % Test,
  "io.github.iltotore" %% "iron" % "3.2.0" % Test,
  "io.github.iltotore" %% "iron-scalacheck" % "3.2.0" % Test,
  "io.github.iltotore" %% "iron-jsoniter" % "3.2.0" % Test,
  "com.lihaoyi" %% "requests" % "0.9.0" % Test
)

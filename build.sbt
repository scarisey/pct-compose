scalaVersion := "3.3.1"

testFrameworks += new TestFramework("org.scalatest.tools.Framework")

libraryDependencies ++= Seq(
  "com.lihaoyi" %% "mainargs" % "0.5.4" ,
  "com.lihaoyi" %% "os-lib" % "0.9.2" ,
  "com.lihaoyi" %% "pprint" % "0.8.1" ,
  "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % "2.25.0" ,
  "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "2.25.0" ,
  "io.github.iltotore" %% "iron" % "2.3.0" ,
  "io.github.iltotore" %% "iron-scalacheck" % "2.3.0" ,
  "io.github.iltotore" %% "iron-jsoniter" % "2.3.0" ,
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


name := "xyniac"

version := "0.1"
resolvers ++= Seq(
  "Typesafe" at "http://repo.typesafe.com/typesafe/releases/",
  "Maven" at "https://repo1.maven.org/maven2/",
)


lazy val commonSettings = Seq(
  organization := "com.xyniac",
  version := "0.1.0-SNAPSHOT"
)

lazy val app = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "fat-jar-test"
  ).enablePlugins()


scalaVersion in ThisBuild := "2.13.0"
libraryDependencies += "org.scala-lang" % "scala-compiler" % "2.13.0"
libraryDependencies += "org.scala-lang" % "scala-reflect" % "2.13.0"


libraryDependencies += "com.google.code.gson" % "gson" % "2.8.6"
libraryDependencies += "org.scala-lang.modules" %% "scala-parallel-collections" % "0.2.0"
libraryDependencies += "org.testng" % "testng" % "7.1.0" % Test
libraryDependencies += "org.scalatest" %% "scalatest" % "3.1.1" % Test
libraryDependencies += "org.json4s" %% "json4s-native" % "3.6.7"
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.30"
libraryDependencies += "org.slf4j" % "slf4j-log4j12" % "1.7.30" % Test
libraryDependencies += "com.github.marschall" % "memoryfilesystem" % "2.1.0" % Test

retrieveManaged := true
updateOptions := updateOptions.value.withCachedResolution(true)

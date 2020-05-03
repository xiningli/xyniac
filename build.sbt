name := "xyniac"

version := "0.1"
resolvers ++= Seq(
  "Typesafe" at "http://repo.typesafe.com/typesafe/releases/",
  "Maven" at "https://repo1.maven.org/maven2/",
)

scalaVersion := "2.13.0"
libraryDependencies += "org.scala-lang" % "scala-compiler" % "2.13.0"
//libraryDependencies += "com.twitter" %% "util-eval" % "6.40.0"
libraryDependencies += "org.scala-lang" % "scala-reflect" % "2.13.0"

retrieveManaged := true

updateOptions := updateOptions.value.withCachedResolution(true)
//XitrumPackage.copy("dirToCopy", "fileToCopy")

libraryDependencies += "com.google.code.gson" % "gson" % "2.8.6"

//addSbtPlugin("com.github.battermann" % "sbt-json" % "0.5.0")
// https://mvnrepository.com/artifact/org.testng/testng
libraryDependencies += "org.testng" % "testng" % "7.1.0" % Test
// https://mvnrepository.com/artifact/org.scalatest/scalatest
libraryDependencies += "org.scalatest" %% "scalatest" % "3.1.1" % Test

// https://mvnrepository.com/artifact/org.apache.commons/commons-collections4
libraryDependencies += "org.apache.commons" % "commons-collections4" % "4.4"
// https://mvnrepository.com/artifact/org.reflections/reflections// https://mvnrepository.com/artifact/org.reflections/reflections

libraryDependencies += "org.reflections" % "reflections" % "0.9.12"
libraryDependencies += "org.json4s" %% "json4s-native" % "3.6.7"
//libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.6.7"
// https://mvnrepository.com/artifact/org.scala-lang/scala-actors
libraryDependencies += "org.scala-lang" % "scala-actors" % "2.11.12"

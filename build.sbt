name := """swap-rss"""
organization := "io.github.go4ble"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.10"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test

libraryDependencies ++= Seq(
  "org.jsoup" % "jsoup" % "1.15.3"
)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "io.github.go4ble.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "io.github.go4ble.binders._"

dockerBaseImage := "openjdk:11"
dockerRepository := Some("ghcr.io/go4ble")
dockerExposedPorts := Seq(9000)
dockerUpdateLatest := true
javaOptions += "-Dpidfile.path=/dev/null"

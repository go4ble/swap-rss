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

import com.typesafe.sbt.packager.docker._
dockerBaseImage := "eclipse-temurin:11-jre-alpine"
dockerRepository := Some("ghcr.io/go4ble")
dockerExposedPorts := Seq(9000)
dockerUpdateLatest := true
dockerCommands := {
  // Update docker commands to install bash right before existing final RUN command
  val insertAt = dockerCommands.value.lastIndexWhere(_.makeContent.startsWith("RUN"))
  dockerCommands.value.patch(insertAt, Seq(Cmd("RUN", "apk", "add", "bash")), 0)
}

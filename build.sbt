organization := "net.ericfredericks"

name := "budgetfree"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.12.0"

scalacOptions ++= Seq("-deprecation", "-feature")

// UI
libraryDependencies ++= Seq(
  "org.scalafx" %% "scalafx" % "8.0.102-R11"
)

// Logging
libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.22",
  "org.slf4j" % "slf4j-simple" % "1.7.22",
  "org.clapper" %% "grizzled-slf4j" % "1.3.0"
)

// Akka
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.16",
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.16"
)

// ScalaTest and Scalactic
libraryDependencies ++= Seq(
  "org.scalactic" %% "scalactic" % "3.0.1",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)

fork := true


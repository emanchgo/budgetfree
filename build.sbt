/*
 *  # Trove
 *
 *  This file is part of Trove - A FREE desktop budgeting application that
 *  helps you track your finances, FREES you from complex budgeting, and
 *  enables you to build your TROVE of savings!
 *
 *  Copyright © 2016-2021 Eric John Fredericks.
 *
 *  Trove is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Trove is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Trove.  If not, see <http://www.gnu.org/licenses/>.
 */

//ejf-fixMe: change to org.kalergic
organization := "net.ericfredericks"
name := "trove"
version := "0.1.0-SNAPSHOT"
scalaVersion := "2.13.5"
scalacOptions ++= Seq("-deprecation", "-feature")
javaOptions ++= Seq("-Djdk.gtk.version=2")
coverageEnabled := true
coverageExcludedPackages := "trove\\.models\\..*;trove\\.event\\..*;trove\\.ui\\..*"
resolvers += "Artima Maven Repository" at "https://repo.artima.com/releases"

Global / onChangedBuildSource := ReloadOnSourceChanges

// Versions
//ejf-fixMe: upgrade akka, stop using classic interface
val akkaVersion          = "2.6.13"

val grizzledSlf4jVersion = "1.3.4"

//ejf-fixMe: drop mockito
val mockitoScalaVersion = "1.16.33"

val scalaFxVersion       = "15.0.1-R21"
val javaFxVersion        = "16"
val slf4jVersion         = "1.7.30"

//ejf-fixMe: replace slick (quill)
val slickVersion         = "3.3.3"

val sqliteJdbcVersion    = "3.34.0"

val scalacticVersion     = "3.2.5"
val scalatestVersion     = "3.2.5"

// UI
libraryDependencies ++= Seq(
  "org.openjfx" % "javafx" % javaFxVersion,
  "org.scalafx" %% "scalafx" % scalaFxVersion
)

// Determine OS version of JavaFX binaries
lazy val osName = System.getProperty("os.name") match {
  case n if n.startsWith("Linux")   => "linux"
  case n if n.startsWith("Mac")     => "mac"
  case n if n.startsWith("Windows") => "win"
  case _ => throw new Exception("Unknown platform!")
}

lazy val javaFXModules = Seq("base", "controls", "fxml", "graphics", "media", "swing", "web")
libraryDependencies ++= javaFXModules.map( m =>
  "org.openjfx" % s"javafx-$m" % "11" classifier osName
)

// Scala
libraryDependencies += "org.scala-lang.modules" %% "scala-collection-compat" % "2.4.3"

// Logging
libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % slf4jVersion,
  "org.slf4j" % "slf4j-simple" % slf4jVersion,
  "org.clapper" %% "grizzled-slf4j" % grizzledSlf4jVersion
)

// Akka
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
)

// Test
libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % scalatestVersion % Test,
  "org.mockito" %% "mockito-scala" % mockitoScalaVersion % Test
)

// Database
libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % slickVersion,
  "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
  "org.xerial" % "sqlite-jdbc" % sqliteJdbcVersion
)

// Utilities
libraryDependencies ++= Seq(
  "org.scalactic" %% "scalactic" % scalacticVersion
)

fork := true


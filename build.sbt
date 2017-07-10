/*
 *  # Trove
 *
 *  This file is part of Trove - A FREE desktop budgeting application that
 *  helps you track your finances, FREES you from complex budgeting, and
 *  enables you to build your TROVE of savings!
 *
 *  Copyright © 2016-2017 Eric John Fredericks.
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


organization := "net.ericfredericks"

name := "trove"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.12.2"

scalacOptions ++= Seq("-deprecation", "-feature")

// Versions
val akkaVersion          = "2.5.3"
val grizzledSlf4jVersion = "1.3.1"
val scalacticVersion     = "3.0.3"
val scalaFxVersion       = "8.0.102-R11"
val scalatestVersion     = "3.0.3"
val slf4jVersion         = "1.7.25"
val slickVersion         = "3.2.0"
val sqliteJdbcVersion    = "3.19.3"

// UI
libraryDependencies ++= Seq(
  "org.scalafx" %% "scalafx" % scalaFxVersion
)

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

// ScalaTest
libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % scalatestVersion
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


// in the name of ALLAH

organization := "com.bisphone"

name := "akkastream"

version := "0.4.2"

scalaVersion := "2.11.11"

scalacOptions ++= Seq("-feature", "-deprecation", "-language:postfixOps")

def akka (
  module: String,
  version: String = "2.5.1"
) = "com.typesafe.akka" %% module % version

fork := true

libraryDependencies ++= Seq(
  akka("akka-actor"),
  akka("akka-stream"),
  "com.bisphone" %% "std" % "0.8.3"
)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.6" % Test,
  "com.bisphone" %% "beta-testkit" % "0.1.0" % Test,
  akka("akka-testkit") % Test,
  akka("akka-stream-testkit") % Test
)

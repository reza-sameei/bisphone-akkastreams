// in the name of ALLAH

organization := "com.bisphone"

name := "akkastream"

version := "0.4.3"

scalaVersion := "2.11.11"

crossScalaVersions := Seq("2.11.11", "2.12.5")

scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    "-language:postfixOps",
    "-language:implicitConversions"
)

def akka(
    module: String,
    version: String = "2.5.6"
) = "com.typesafe.akka" %% module % version

fork := true

libraryDependencies ++= Seq(
    akka("akka-actor"),
    akka("akka-stream"),
    "com.bisphone" %% "std" % "0.12.0"
)

libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.0.4" % Test,
    "com.bisphone" %% "testkit" % "0.4.1" % Test,
    akka("akka-testkit") % Test,
    akka("akka-stream-testkit") % Test
)

// in the name of ALLAH

organization := "com.bisphone"

name := "akkastream"

version := "0.4.4"

scalaVersion := "2.11.12"

crossScalaVersions := Seq("2.11.12", "2.12.6")

scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    "-language:postfixOps",
    "-language:implicitConversions"
)

def akka(
    module: String,
    version: String = "2.5.17"
) = "com.typesafe.akka" %% module % version

fork := true

libraryDependencies ++= Seq(
    akka("akka-actor"),
    akka("akka-stream"),
    "com.bisphone" %% "std" % "0.13.0"
)

libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.0.4" % Test,
    "com.bisphone" %% "testkit" % "0.4.2" % Test,
    akka("akka-testkit") % Test,
    akka("akka-stream-testkit") % Test
)

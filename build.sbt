organization := "com.micahw"

name := "scala-bluetooth"

version := "0.0.1"

scalaVersion := "2.11.8"

resolvers += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/maven-releases/"

libraryDependencies += "ch.qos.logback" %  "logback-classic" % "1.1.7"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0"

unmanagedJars in Compile += file("/usr/share/java/dbus-2.8.jar")

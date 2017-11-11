organization := "com.micahw"

name := "scala-bluetooth"

version := "0.0.2"

crossScalaVersions := Seq("2.11.8", "2.12.4")

resolvers += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/maven-releases/"

libraryDependencies += "ch.qos.logback" %  "logback-classic" % "1.2.3"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2"

unmanagedJars in Compile += file("/usr/share/java/dbus-2.8.jar")

// settings for publishing to maven central
publishMavenStyle := true
publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

pomExtra := (
  <url>https://github.com/mwylde/scala-bluetooth</url>
  <licenses>
    <license>
      <name>MIT</name>
      <url>https://opensource.org/licenses/MIT</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:mwylde/scala-bluetooth.git</url>
    <connection>scm:git:git@github.com:mwylde/scala-bluetooth.git</connection>
  </scm>
  <developers>
    <developer>
      <id>mwylde</id>
      <name>Micah Wylde</name>
      <url>http://micahw.com</url>
    </developer>
  </developers>)

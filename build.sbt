import com.typesafe.startscript.StartScriptPlugin

seq(StartScriptPlugin.startScriptForClassesSettings: _*)

name := "session-tester"

version := "1.0"

scalaVersion := "2.9.1"

resolvers ++= Seq("twitter.com" at "http://maven.twttr.com", "coda" at "http://repo.codahale.com")

libraryDependencies ++= Seq(
	"com.twitter" % "finagle-core_2.9.1" % "1.10.0" withSources(),
	"com.twitter" % "finagle-http_2.9.1" % "1.10.0" withSources(),
	"com.twitter" % "util-logging_2.9.1" % "1.12.12" withSources(),
	"com.codahale" % "jerkson_2.9.1" % "0.5.0" withSources()
)




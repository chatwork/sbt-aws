sonatypeProfileName := "org.sisioh"

organization in ThisBuild := "org.sisioh"

sbtPlugin := true

name := "sbt-aws-plugin"

description := "TODO"

publishMavenStyle := false

scalaVersion := "2.10.5"

val aws4sVersion = "1.0.5"

val awsSdkVersion = "1.9.22"

resolvers ++= Seq(
  "Sonatype OSS Snapshot Repository" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Sonatype OSS Release Repository" at "https://oss.sonatype.org/content/repositories/releases/",
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= Seq(
  "org.sisioh" %% "aws4s-core" % aws4sVersion withSources(),
  "org.sisioh" %% "aws4s-s3" % aws4sVersion withSources(),
  "org.sisioh" %% "aws4s-eb" % aws4sVersion withSources(),
  "org.sisioh" %% "aws4s-cfn" % aws4sVersion withSources(),
  "org.sisioh" %% "sisioh-config" % "0.0.4" withSources(),
  "commons-codec" % "commons-codec" % "1.8",
  "commons-io" % "commons-io" % "2.4"
)

shellPrompt := {
  "sbt (%s)> " format projectId(_)
}

def projectId(state: State) = extracted(state).currentProject.id

def extracted(state: State) = Project extract state


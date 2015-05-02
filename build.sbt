lazy val commonSettings = Seq(
  version in ThisBuild := "1.0.0-SNAPSHOT",
  organization in ThisBuild := "com.github.j5ik2o"
)

val aws4sVersion = "1.0.2-SNAPSHOT"

val awsSdkVersion = "1.9.22"

lazy val root = (project in sbt.file(".")).
  settings(commonSettings: _*).
  settings(
    sbtPlugin := true,
    name := "sbt-aws-plugin",
    description := "TODO",
    publishMavenStyle := false,
    scalaVersion := "2.10.5",
    resolvers ++= Seq(
      "Sonatype OSS Snapshot Repository" at "https://oss.sonatype.org/content/repositories/snapshots/",
      "Sonatype OSS Release Repository" at "https://oss.sonatype.org/content/repositories/releases/",
      "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
    ),
    libraryDependencies ++= Seq(
      "org.sisioh" %% "aws4s-s3" % aws4sVersion withSources(),
      "commons-codec" % "commons-codec" % "1.8",
      "commons-io" % "commons-io" % "2.4"
    )
  )


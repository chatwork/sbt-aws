import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._
import xerial.sbt.Sonatype.autoImport._

val releaseSettings = Seq(
  releaseCrossBuild := true,
  releaseTagName := { (version in ThisBuild).value },
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("^ publishSigned"),
  setNextVersion,
  commitNextVersion,
  releaseStepCommand("sonatypeReleaseAll"),
  pushChanges
  )
)

val aws4sVersion = "1.0.16"

val sisiohConfigVersion = "0.0.15"
val sbtCrossVersion = sbtVersion in pluginCrossBuild

lazy val baseSettings = Seq(
  scalaVersion := (CrossVersion partialVersion sbtCrossVersion.value match {
    case Some((0, 13)) => "2.10.6"
    case Some((1, _))  => "2.12.4"
    case _             => sys error s"Unhandled sbt version ${sbtCrossVersion.value}"
  }),
  sonatypeProfileName := "com.chatwork",
  organization := "com.chatwork",
  shellPrompt := {
    "sbt (%s)> " format projectId(_)
  },
  publishMavenStyle := true,
  publishArtifact in Test := false,
  publishTo := sonatypePublishTo.value,
  pomIncludeRepository := {
    _ => false
  },
  pomExtra := {
    <url>https://github.com/chatwork/sbt-aws</url>
      <licenses>
        <license>
          <name>The MIT License</name>
          <url>http://opensource.org/licenses/MIT</url>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:chatwork/sbt-aws.git</url>
        <connection>scm:git:github.com/chatwork/sbt-aws</connection>
        <developerConnection>scm:git:git@github.com:chatwork/sbt-aws.git</developerConnection>
      </scm>
      <developers>
        <developer>
          <id>cw-junichikato</id>
          <name>Junichi Kato</name>
        </developer>
      </developers>
  },
  credentials += Credentials((baseDirectory in LocalRootProject).value / ".credentials"),
  scalacOptions -= "-Ybackend:GenBCode"
)

lazy val pluginSettings = baseSettings ++ releaseSettings ++ Seq(
  sbtPlugin := true,
  crossSbtVersions := Seq("0.13.16", "1.0.0"),
  resolvers ++= Seq(
    "Sonatype OSS Snapshot Repository" at "https://oss.sonatype.org/content/repositories/snapshots/",
    "Sonatype OSS Release Repository" at "https://oss.sonatype.org/content/repositories/releases/",
    "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
  ),
  libraryDependencies ++= Seq(
  ),
  scriptedBufferLog := false,
  scriptedLaunchOpts := {
    scriptedLaunchOpts.value ++
      Seq("-Xmx1024M", "-Dproject.version=" + version.value)
  },
  scriptedBufferLog := false
)


lazy val core = (project in file("sbt-aws-core")).settings(pluginSettings: _*).settings(
  name := "sbt-aws-core",
  libraryDependencies ++= Seq(
    "org.sisioh" %% "sisioh-config" % sisiohConfigVersion,
    "org.sisioh" %% "aws4s-core" % aws4sVersion,
    "commons-io" % "commons-io" % "2.4"
  )
)

lazy val s3 = (project in file("sbt-aws-s3")).settings(pluginSettings: _*).settings(
  name := "sbt-aws-s3",
  libraryDependencies ++= Seq(
    "org.sisioh" %% "aws4s-s3" % aws4sVersion
  )
).dependsOn(core)

lazy val s3Resolver = (project in file("sbt-aws-s3-resolver")).settings(pluginSettings: _*).settings(
  name := "sbt-aws-s3-resolver",
  libraryDependencies ++= Seq(
    "org.apache.ivy" % "ivy" % "2.4.0",
    "io.get-coursier" %% "coursier" % "1.0.0-RC3",
    "io.get-coursier" %% "coursier-cache" % "1.0.0-RC3",
    "org.slf4j" % "slf4j-api" % "1.7.25",
    "ch.qos.logback" % "logback-classic" % "1.2.3"
  )
).dependsOn(s3)

lazy val eb = (project in file("sbt-aws-eb")).settings(pluginSettings: _*).settings(
  name := "sbt-aws-eb",
  libraryDependencies ++= Seq(
    "org.sisioh" %% "aws4s-eb" % aws4sVersion,
    "org.freemarker" % "freemarker" % "2.3.23"
  )
).dependsOn(s3)

lazy val cfn = (project in file("sbt-aws-cfn")).settings(pluginSettings: _*).settings(
  name := "sbt-aws-cfn",
  libraryDependencies ++= Seq(
    "org.sisioh" %% "aws4s-cfn" % aws4sVersion
  )
).dependsOn(s3)

lazy val root = (project in file(".")).settings(baseSettings: _*).settings(
  name := "sbt-aws"
).aggregate(core, s3, eb, cfn, s3Resolver)

def projectId(state: State) = extracted(state).currentProject.id

def extracted(state: State) = Project extract state

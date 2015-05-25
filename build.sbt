import sbt.ScriptedPlugin._

import scalariform.formatter.preferences._

val aws4sVersion = "1.0.7"

val sisiohConfigVersion = "0.0.7"

lazy val baseSettings = Seq(
  scalaVersion := "2.10.5",
  sonatypeProfileName := "com.chatwork",
  organization in ThisBuild := "com.chatwork",
  shellPrompt := {
    "sbt (%s)> " format projectId(_)
  },
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := {
    _ => false
  },
  pomExtra := {
    <url>https://github.com/chatwork/sbt-aws</url>
      <licenses>
        <license>
          <name>Apache 2</name>
          <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
        </license>
      </licenses>
      <scm>
        <url>git@github.com:chatwork/sbt-aws.git</url>
        <connection>scm:git:github.com/chatwork/sbt-aws</connection>
        <developerConnection>scm:git:git@github.com:chatwork/sbt-aws.git</developerConnection>
      </scm>
      <developers>
        <developer>
          <id>j5ik2o</id>
          <name>Junichi Kato</name>
          <url>http://j5ik2o.me/</url>
        </developer>
      </developers>
  }
)

lazy val pluginSettings = baseSettings ++ ScriptedPlugin.scriptedSettings ++ scalariformSettings ++ Seq(
  sbtPlugin := true,
  resolvers ++= Seq(
    "Sonatype OSS Snapshot Repository" at "https://oss.sonatype.org/content/repositories/snapshots/",
    "Sonatype OSS Release Repository" at "https://oss.sonatype.org/content/repositories/releases/",
    "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
  ),
  libraryDependencies ++= Seq(
  ),
  scriptedLaunchOpts := {
    scriptedLaunchOpts.value ++
      Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
  },
  scriptedBufferLog := false,
  ScalariformKeys.preferences :=
    ScalariformKeys.preferences.value
      .setPreference(AlignParameters, true)
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(DoubleIndentClassDeclaration, true)
      .setPreference(PreserveDanglingCloseParenthesis, true)
      .setPreference(MultilineScaladocCommentsStartOnFirstLine, false),
  credentials := {
    val ivyCredentials = (baseDirectory in LocalRootProject).value / ".credentials"
    Credentials(ivyCredentials) :: Nil
  }
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

lazy val eb = (project in file("sbt-aws-eb")).settings(pluginSettings: _*).settings(
  name := "sbt-aws-eb",
  libraryDependencies ++= Seq(
    "org.sisioh" %% "aws4s-eb" % aws4sVersion
  )
).dependsOn(s3)

lazy val cfn = (project in file("sbt-aws-cfn")).settings(pluginSettings: _*).settings(
  name := "sbt-aws-cfn",
  libraryDependencies ++= Seq(
    "org.sisioh" %% "aws4s-cfn" % aws4sVersion
  )
).dependsOn(s3)

def projectId(state: State) = extracted(state).currentProject.id

def extracted(state: State) = Project extract state
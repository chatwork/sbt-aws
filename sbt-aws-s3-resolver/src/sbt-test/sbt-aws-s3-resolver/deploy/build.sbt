organization := "com.chatwork"

name := "sbt-aws-s3-resolver-deploy-test"

version := "0.0.3-SNAPSHOT"

s3Acl in aws := com.amazonaws.services.s3.model.CannedAccessControlList.Private

credentialProfileName in aws := Some("maven-test")

s3OverwriteObject in aws := isSnapshot.value

publishMavenStyle := true

s3DeployStyle in aws := DeployStyle.Maven

publishArtifact in Test := false

pomIncludeRepository := {
  _ => false
}

pomExtra := {
  <url>https://github.com/chatwork/sbt-aws</url>
    <licenses>
      <license>
        <name>ChatWork License</name>
        <url>http://www.chatwork.com/</url>
      </license>
    </licenses>
    <scm>
      <url>https://github.com/chatwork/sbt-aws</url>
      <connection>scm:git:github.com/chatwork/sbt-aws</connection>
      <developerConnection>scm:git@github.com:chatwork/sbt-aws.git</developerConnection>
    </scm>
    <developers>
      <developer>
        <id>cw-junichikato</id>
        <name>Junichi Kato</name>
      </developer>
    </developers>
}

publishTo := {
  if (isSnapshot.value)
    Some(
      (s3Resolver in aws).value("ChatWork's Maven Snapshot Repository", "s3://tky-chatwork-inhouse-maven-repository/snapshots")
    )
  else
    Some(
      (s3Resolver in aws).value("ChatWork's Maven Release Repository", "s3://tky-chatwork-inhouse-maven-repository/releases")
    )
}



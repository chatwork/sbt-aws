name := "sbt-aws-s3-resolver-resolve-test"

s3Acl in aws := com.amazonaws.services.s3.model.CannedAccessControlList.Private

credentialProfileName in aws := Some("maven-test")

resolvers ++= Seq(
  "ChatWork's Maven Snapshot Repository" at "s3://tky-chatwork-inhouse-maven-repository/snapshots",
  "ChatWork's Maven Release Repository" at "s3://tky-chatwork-inhouse-maven-repository/releases"
)

libraryDependencies += "com.chatwork" %% "sbt-aws-s3-resolver-deploy-test" % "0.0.1-SNAPSHOT"

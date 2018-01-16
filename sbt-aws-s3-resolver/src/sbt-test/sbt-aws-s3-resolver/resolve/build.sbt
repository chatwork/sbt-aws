
name := "sbt-aws-s3-resolver-resolve-test"

s3Acl in aws := com.amazonaws.services.s3.model.CannedAccessControlList.Private

credentialProfileName in aws := Some("maven-test")

resolvers ++= Seq[Resolver](
  (s3Resolver in aws).value("ChatWork's Maven Snapshot Repository", "s3://tky-chatwork-inhouse-maven-repository/snapshots"),
  (s3Resolver in aws).value("ChatWork's Maven Release Repository", "s3://tky-chatwork-inhouse-maven-repository/releases")
)

libraryDependencies += "com.chatwork" %% "sbt-aws-s3-resolver-deploy-test" % "0.0.3-SNAPSHOT"

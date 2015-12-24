credentialProfileName in aws := Some("sbt-aws-scripted-test")

ebBundleTargetFiles in aws <<= Def.task {
  val base = baseDirectory.value
  val packageJarFile = (packageBin in Compile).value
  Seq(
    (base / "Dockerfile", "Dockerfile"),
    (base / "Dockerrun.aws.json", "Dockerrun.aws.json"),
    (packageJarFile, packageJarFile.name)
  )
}

ebApplicationName in aws := (name in thisProjectRef).value + "-" + new java.util.Date().getTime.toString

ebEnvironmentName in aws := new java.util.Date().getTime.toString + "-env"

ebS3BucketName in aws := Some("sbt-aws-eb-test")

ebS3CreateBucket in aws := true

ebSolutionStackName in aws := Some("64bit Amazon Linux 2015.09 v2.0.4 running Docker 1.7.1")

ebApplicationVersionCreateOrUpdateAndWait in aws <<= (ebApplicationVersionCreateOrUpdateAndWait in aws) dependsOn (ebApplicationCreateOrUpdateAndWait in aws)

ebEnvironmentCreateOrUpdateAndWait in aws <<= (ebEnvironmentCreateOrUpdateAndWait in aws) dependsOn (ebApplicationVersionCreateOrUpdateAndWait in aws)

val deploy = taskKey[Unit]("deploy")

deploy := (ebEnvironmentCreateOrUpdateAndWait in aws).value
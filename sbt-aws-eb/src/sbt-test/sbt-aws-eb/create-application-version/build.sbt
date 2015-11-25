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

ebS3BucketName in aws := Some("sbt-aws-eb-test")

ebS3CreateBucket in aws := true

ebAutoCreateApplication in aws := Some(true)

credentialProfileName in aws := Some("sbt-aws")

ebBundleTargetFiles in aws := Def.task {
  val base = baseDirectory.value
  val packageJarFile = (packageBin in Compile).value
  Seq(
    (base / "Dockerfile", "Dockerfile"),
    (base / "Dockerrun.aws.json", "Dockerrun.aws.json"),
    (packageJarFile, packageJarFile.name)
  )
}.value

ebApplicationName in aws := (name in thisProjectRef).value + "-" + new java.util.Date().getTime.toString

ebEnvironmentName in aws := new java.util.Date().getTime.toString + "-env"

ebS3BucketName in aws := Some("sbt-aws-eb-cct-test")

ebS3CreateBucket in aws := true

ebAutoCreateApplication in aws := Some(true)

ebEnvironmentUseVersionLabel in aws := Some((ebApplicationVersionLabel in aws).value)

ebConfigurationTemplateName in aws :=  Some("ct-" + new java.util.Date().getTime.toString)

ebConfigurationTemplate in aws := {
  (ebConfigurationTemplateName in aws).value.map{ templateName =>
    EbConfigurationTemplate(
      name = templateName,
      description = Some("test"),
      solutionStackName = "64bit Amazon Linux 2015.09 v2.0.6 running Docker 1.7.1",
      optionSettings = Seq.empty,
      optionsToRemoves = Seq.empty,
      recreate = true
    )
  }
}

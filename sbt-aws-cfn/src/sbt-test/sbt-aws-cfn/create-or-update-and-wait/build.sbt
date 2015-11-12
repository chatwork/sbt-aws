region in aws := com.amazonaws.regions.Regions.AP_NORTHEAST_1

credentialProfileName in aws := Some("sbt-aws-scripted-test")

//cfnS3BucketName in aws := Some("sbt-aws-cw-cfn-template-test")

cfnS3KeyMapper in aws := { key: String =>
  val projectName = (name in thisProjectRef).value
  s"$projectName/$key"
}


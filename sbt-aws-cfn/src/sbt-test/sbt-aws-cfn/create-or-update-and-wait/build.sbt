region in aws := com.amazonaws.regions.Regions.AP_NORTHEAST_1

cfnS3BucketName in aws := Some("cw-cfn-template")

cfnS3KeyMapper in aws := { key: String =>
  val projectName = (name in thisProjectRef).value
  s"$projectName/$key"
}


lazy val root = (project in sbt.file(".")).
  enablePlugins(SbtAwsPlugin).
  settings(
    logLevel := Level.Info,
    version := "0.1",
    scalaVersion := "2.10.5",
    AwsKeys.region in AwsKeys.aws := com.amazonaws.regions.Regions.AP_NORTHEAST_1,
    AwsKeys.CfnKeys.cfnStackName := "S3BucketTest",
    AwsKeys.CfnKeys.cfnStackParams := Map("S3BucketName" -> "hogehoge-hogehoge-hogehoge")
  )

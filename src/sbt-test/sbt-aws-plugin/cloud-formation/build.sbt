lazy val root = (project in sbt.file(".")).
  enablePlugins(SbtAwsPlugin).
  settings(
    logLevel := Level.Info,
    version := "0.1",
    scalaVersion := "2.10.5",
    region in aws := com.amazonaws.regions.Regions.AP_NORTHEAST_1
//    cfnStackName in aws := "test"
//    cfnStackParams in aws := Map("S3BucketName" -> "hogehoge-hogehoge-hogehoge")
  )

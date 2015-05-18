
lazy val root = (project in sbt.file(".")).
  enablePlugins(SbtAwsPlugin).
  settings(
    logLevel := Level.Info,
    version := "0.1",
    scalaVersion := "2.10.5",
    region in aws := com.amazonaws.regions.Regions.AP_NORTHEAST_1,
    s3BucketName in aws := "aws-sbt-plugin-s3-test",
    s3Key in aws := "test",
    s3File in aws := Some(sbt.file("build.sbt")),
    s3OverwriteObject in aws := true,
    s3CreateBucket in aws := true
  )

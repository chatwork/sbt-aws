
lazy val root = (project in sbt.file(".")).
  enablePlugins(SbtAwsPlugin).
  settings(
    version := "0.1",
    scalaVersion := "2.10.5",
    AWS.S3.bucketName := "cw-s3-test",
    AWS.S3.key := "test",
    AWS.S3.file := sbt.file("build.sbt")
  )

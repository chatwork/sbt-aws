s3BucketName in aws := "sbt-aws-s3"

s3Key in aws := "build.sbt"

s3File in aws := Some(file("build.sbt"))

s3OverwriteObject in aws := true

s3CreateBucket in aws := true
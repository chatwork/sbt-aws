# sbt-aws-s3

This SBT plugin adds support for using Amazon S3.

## Installation

Add this to your project/plugins.sbt file:

```scala
resolvers += "Sonatype OSS Release Repository" at "https://oss.sonatype.org/content/repositories/releases/"

addSbtPlugin("com.chatwork" % "sbt-aws-s3" % "1.0.32")
```

## Usage

- Set the value to the setting item into build.sbt.

```scala
region in aws := com.amazonaws.regions.Regions.AP_NORTHEAST_1

s3BucketName in aws := "sbt-aws-s3"

s3Key in aws := "build.sbt"

s3File in aws := Some(file("build.sbt"))

s3OverwriteObject in aws := true

s3CreateBucket in aws := true
```

- Upload file

```sh
$ sbt aws::s3Upload
[info] put object request : bucketName = sbt-aws-s3, key = build.sbt, file = build.sbt, overwrite = true, createBucket = true
[info] put object requested : bucketName = sbt-aws-s3 key = build.sbt, url = https://sbt-aws-s3.s3-ap-northeast-1.amazonaws.com/build.sbt
[success] Total time: 2 s, completed 2015/05/26 13:00:50
```

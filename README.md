# sbt-aws

[![Build Status](https://travis-ci.org/chatwork/sbt-aws.svg)](https://travis-ci.org/chatwork/sbt-aws)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.chatwork/sbt-aws_2.10/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.chatwork/sbt-aws_2.10)
[![Scaladoc](http://javadoc-badge.appspot.com/com.chatwork/sbt-aws.svg?label=scaladoc)](http://javadoc-badge.appspot.com/com.chatwork/sbt-aws_2.10)
[![Reference Status](https://www.versioneye.com/java/com.chatwork:sbt-aws_2.11/reference_badge.svg?style=flat)](https://www.versioneye.com/java/com.chatwork:sbt-aws_2.10/references)

## Installation

Add the following to your `project/plugin.sbt` (Scala 2.10.x, and Scala 2.11.x):

### Release Version

```scala
resolvers += "Sonatype OSS Release Repository" at "https://oss.sonatype.org/content/repositories/releases/"

addSbtPlugin("com.chatwork" % "sbt-aws-s3" % "1.0.19")

addSbtPlugin("com.chatwork" % "sbt-aws-cfn" % "1.0.19")

addSbtPlugin("com.chatwork" % "sbt-aws-eb" % "1.0.19")
```

### Snapshot Version

```scala
resolvers += "Sonatype OSS Snapshot Repository" at "https://oss.sonatype.org/content/repositories/snapshots/"

addSbtPlugin("com.chatwork" % "sbt-aws-s3" % "1.0.20-SNAPSHOT")

addSbtPlugin("com.chatwork" % "sbt-aws-cfn" % "1.0.20-SNAPSHOT")

addSbtPlugin("com.chatwork" % "sbt-aws-eb" % "1.0.20-SNAPSHOT")
```

## Usage


#### sbt-aws-eb for ElasticBeanstalk

- Please set the required files to the application bundle to ebBundleTargetFiles.

```scala
region in aws := com.amazonaws.regions.Regions.AP_NORTHEAST_1

ebBundleTargetFiles in aws <<= Def.task {
  val base = baseDirectory.value
  val packageJarFile = (packageBin in Compile).value
  Seq(
    (base / "Dockerfile", "Dockerfile"),
    (base / "Dockerrun.aws.json", "Dockerrun.aws.json"),
    (packageJarFile, packageJarFile.name)
  )
}

ebS3BucketName in aws := "sbt-aws-eb"

ebS3CreateBucket in aws := true // if necessary
```

- Build application-bundle

```sh
$ sbt aws::ebBuildBundle
[info] Updating {file:/Users/j5ik2o/sbt-aws/sbt-aws-eb/src/sbt-test/sbt-aws-eb/build-bundle/}build-bundle...
[info] Resolving org.fusesource.jansi#jansi;1.4 ...
[info] Done updating.
[info] Compiling 1 Scala source to /Users/j5ik2o/sbt-aws/sbt-aws-eb/src/sbt-test/sbt-aws-eb/build-bundle/target/scala-2.10/classes...
[info] Packaging /Users/j5ik2o/sbt-aws/sbt-aws-eb/src/sbt-test/sbt-aws-eb/build-bundle/target/scala-2.10/build-bundle_2.10-0.1-SNAPSHOT.jar ...
[info] Done packaging.
[info] created application-bundle: /Users/j5ik2o/sbt-aws/sbt-aws-eb/src/sbt-test/sbt-aws-eb/build-bundle/target/build-bundle-bundle.zip
```

- Upload application-bundle

```sh
$ sbt aws::ebUploadBundle
[info] Updating {file:/Users/j5ik2o/sbt-aws/sbt-aws-eb/src/sbt-test/sbt-aws-eb/upload-bundle/}upload-bundle...
[info] Resolving org.fusesource.jansi#jansi;1.4 ...
[info] Done updating.
[info] Compiling 1 Scala source to /Users/j5ik2o/sbt-aws/sbt-aws-eb/src/sbt-test/sbt-aws-eb/upload-bundle/target/scala-2.10/classes...
[info] Packaging /Users/j5ik2o/sbt-aws/sbt-aws-eb/src/sbt-test/sbt-aws-eb/upload-bundle/target/scala-2.10/upload-bundle_2.10-0.1-SNAPSHOT.jar ...
[info] Done packaging.
[info] created application-bundle: /Users/j5ik2o/sbt-aws/sbt-aws-eb/src/sbt-test/sbt-aws-eb/upload-bundle/target/upload-bundle-bundle.zip
[info] upload /Users/j5ik2o/sbt-aws/sbt-aws-eb/src/sbt-test/sbt-aws-eb/upload-bundle/target/upload-bundle-bundle.zip to sbt-aws-eb/upload-bundle/upload-bundle-0.1-SNAPSHOT-20150525_172404.zip
```
            
#### sbt-aws-cfn for CloudFormation

- Put the CloudFormation template files in the 'aws/cfn/templates' folder

```
aws/cfn/templates
  + infra.template
```

- Set the value to the setting item into build.sbt.


```scala
region in aws := com.amazonaws.regions.Regions.AP_NORTHEAST_1

cfnStackName in aws := Some("example-stack")
```

- Create or update stack.

```sh
$ sbt aws::cfnStackCreateOrUpdateAndWait // wait to complete deploy
[info] upload /Users/j5ik2o/sbt-aws/sbt-aws-cfn/src/sbt-test/sbt-aws-cfn/create-or-update-and-wait/aws/cfn/templates/S3.template to cfn-template/create-or-update-and-wait/create-or-update-and-wait-0.1-SNAPSHOT-20150525_185939.templete
[info] create stack request : stackName = test1, templateUrl = https://cfn-template.s3-ap-northeast-1.amazonaws.com/create-or-update-and-wait/create-or-update-and-wait-0.1-SNAPSHOT-20150525_185939.templete, capabilities = List(), stackParams = Map(S3BucketName1 -> dmmy-test-00002, S3BucketName0 -> dmmy-test-00001), tags = Map()
{StackId: arn:aws:cloudformation:ap-northeast-1:327747897717:stack/test1/c1fdf600-02c4-11e5-a322-506cf9a1c096}
[info] create stack requested : test1 / arn:aws:cloudformation:ap-northeast-1:327747897717:stack/test1/c1fdf600-02c4-11e5-a322-506cf9a1c096
[info] status = CREATE_IN_PROGRESS
[info] status = CREATE_IN_PROGRESS
[info] status = CREATE_IN_PROGRESS
[info] status = CREATE_IN_PROGRESS
[info] status = CREATE_IN_PROGRESS
[info] status = CREATE_IN_PROGRESS
[success] Total time: 25 s, completed 2015/05/25 19:00:04
```

- Delete stack

```sh
$ sbt aws::cfnStackDeleteAndWait
[info] delete stack request   : test1
[info] delete stack requested : test1
[info] status = DELETE_IN_PROGRESS
[info] status = DELETE_IN_PROGRESS
[info] status = DELETE_IN_PROGRESS
[info] status = DELETE_IN_PROGRESS
[info] status = DELETE_IN_PROGRESS
[success] Total time: 25 s, completed 2015/05/25 20:00:04
```

#### sbt-aws-s3 for S3

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


#### Configuration File Support by Typesafe Config

`env/dev.conf` is loaded, in defaults.

```
"aws": {

  "cfnStackName": "test1"

  "cfnStackParams": {
    "S3BucketName0": "test1-dmmy-test-00001",
    "S3BucketName1": "test1-dmmy-test-00002"
  }

}
```

The above is the same mean as the following.

```scala
cfnStackName in aws := "test1",
cfnStackParams in aws := Map(
   "S3BucketName0" -> "test1-dmmy-test-00001",
   "S3BucketName1" -> "test1-dmmy-test-00002",
)
```

#### Profile Function

You can switch the configuration file by specifying the `-Daws.env`.

`env/staging.conf` is loaded.


```sh
$ sbt -Daws.env=staging aws::CfnStackCreateOrUpdate
```

  

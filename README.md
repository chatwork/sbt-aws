# sbt-aws

[![Build Status](https://travis-ci.org/chatwork/sbt-aws.svg)](https://travis-ci.org/chatwork/sbt-aws)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.chatwork/sbt-aws_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.chatwork/sbt-aws_2.11)
[![Scaladoc](http://javadoc-badge.appspot.com/com.chatwork/sbt-aws.svg?label=scaladoc)](http://javadoc-badge.appspot.com/com.chatwork/sbt-aws_2.11)
[![Reference Status](https://www.versioneye.com/java/com.chatwork:sbt-aws_2.11/reference_badge.svg?style=flat)](https://www.versioneye.com/java/com.chatwork:sbt-aws_2.11/references)

## Installation

Add the following to your `project/plugin.sbt` (Scala 2.10.x, and Scala 2.11.x):

### Release Version

```scala
resolvers += "Sonatype OSS Release Repository" at "https://oss.sonatype.org/content/repositories/releases/"

addSbtPlugin("com.chatwork" %% "sbt-aws-s3" % "1.0.6)"

addSbtPlugin("com.chatwork" %% "sbt-aws-cfn" % "1.0.6)"

addSbtPlugin("com.chatwork" %% "sbt-aws-eb" % "1.0.6)"
```

### Snapshot Version

```scala
resolvers += "Sonatype OSS Snapshot Repository" at "https://oss.sonatype.org/content/repositories/snapshots/"

addSbtPlugin("com.chatwork" %% "sbt-aws-s3" % "1.0.7-SNAPSHOT)"

addSbtPlugin("com.chatwork" %% "sbt-aws-cfn" % "1.0.7-SNAPSHOT)"

addSbtPlugin("com.chatwork" %% "sbt-aws-eb" % "1.0.7-SNAPSHOT)"
```

## Usage

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

#### sbt-aws-eb for ElasticBeanstalk

- Please set the required files to the application bundle to ebBundleTargetFiles.

```scala
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
val root = (project in file(".").settings(
    region in aws := com.amazonaws.regions.Regions.AP_NORTHEAST_1,
  

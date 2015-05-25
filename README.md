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

addSbtPlugin("com.chatwork" % "sbt-aws-s3" % "1.0.0")

addSbtPlugin("com.chatwork" % "sbt-aws-cfn" % "1.0.0")

addSbtPlugin("com.chatwork" % "sbt-aws-eb" % "1.0.0")
```

### Snapshot Version

```scala
resolvers += "Sonatype OSS Snapshot Repository" at "https://oss.sonatype.org/content/repositories/snapshots/"

addSbtPlugin("com.chatwork" % "sbt-aws-s3" % "1.0.0-SNAPSHOT")

addSbtPlugin("com.chatwork" % "sbt-aws-cfn" % "1.0.0-SNAPSHOT")

addSbtPlugin("com.chatwork" % "sbt-aws-eb" % "1.0.0-SNAPSHOT")
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


#### sbt-aws-cfn for CloudFormation

- Put the CloudFormation template files in the 'aws/cfn/templates' folder

```
aws/cfn/templates
  + s3.template
  + rds.template
```

- Set the value to the setting item into build.sbt.

```scala
val root = (project in file(".").settings(
    region in aws := com.amazonaws.regions.Regions.AP_NORTHEAST_1,
  
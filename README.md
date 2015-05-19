# sbt-aws-plugin

[![Build Status](https://travis-ci.org/sisioh/sbt-aws-plugin.svg)](https://travis-ci.org/sisioh/sbt-aws-plugin)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.sisioh/sbt-aws-plugin_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.sisioh/sbt-aws-plugin_2.11)
[![Scaladoc](http://javadoc-badge.appspot.com/org.sisioh/sbt-aws-plugin.svg?label=scaladoc)](http://javadoc-badge.appspot.com/org.sisioh/sbt-aws-plugin_2.11)
[![Reference Status](https://www.versioneye.com/java/org.sisioh:sbt-aws-plugin_2.11/reference_badge.svg?style=flat)](https://www.versioneye.com/java/org.sisioh:sbt-aws-plugin_2.11/references)

## Installation

Add the following to your sbt build (Scala 2.10.x, and Scala 2.11.x):

### Release Version

```scala
resolvers += "Sonatype OSS Release Repository" at "https://oss.sonatype.org/content/repositories/releases/"

libraryDependencies += "org.sisioh" %% "sbt-aws-plugin" % "1.0.2"
```

### Snapshot Version

```scala
resolvers += "Sonatype OSS Snapshot Repository" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies += "org.sisioh" %% "sbt-aws-plugin" % "1.0.2-SNAPSHOT"
```

## Usage

### Configuration File Support by Typesafe Config

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

### Profile Function

You can switch the configuration file by specifying the `-Dsbt.aws.profile`.

`env/staging.conf` is loaded.


```sh
$ sbt -Dsbt.aws.profile=staging aws::CfnStackCreateOrUpdate
```


### CloudFormation Functions

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
    cfnStackName in aws := "stackA",
    cfnStackParams in aws := Map("key" -> "value")
)

```

- Create the stack, or Update it

```sh
$ sbt aws::CfnStackCreateOrUpdate
```


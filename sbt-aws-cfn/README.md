# sbt-aws-cfn

This SBT plugin adds support for using Amazon Cloud Formation.

## Installation

Add this to your project/plugins.sbt file:

```scala
resolvers += "Sonatype OSS Release Repository" at "https://oss.sonatype.org/content/repositories/releases/"

addSbtPlugin("com.chatwork" % "sbt-aws-cfn" % "1.0.28")
```

## Usage

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

#### Profile Function

You can switch the configuration file by specifying the `-Daws.env`.

`env/staging.conf` is loaded.


```sh
$ sbt -Daws.env=staging aws::CfnStackCreateOrUpdate
```

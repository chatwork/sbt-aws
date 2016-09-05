# sbt-aws-eb

This SBT plugin adds support for using Amazon Elastic Beanstalk.

## Installation

Add this to your project/plugins.sbt file:

```scala
resolvers += "Sonatype OSS Release Repository" at "https://oss.sonatype.org/content/repositories/releases/"

addSbtPlugin("com.chatwork" % "sbt-aws-eb" % "0.0.24-SNAPSHOT")
```

### Usage

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

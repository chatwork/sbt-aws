# sbt-aws-eb

This SBT plugin adds support for using Amazon Elastic Beanstalk.

## Installation

Add this to your project/plugins.sbt file:

```scala
resolvers += "Sonatype OSS Release Repository" at "https://oss.sonatype.org/content/repositories/releases/"

addSbtPlugin("com.chatwork" % "sbt-aws-eb" % "1.0.35")
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
```

- Upload application-bundle

```sh
$ sbt aws::ebUploadBundle
```

- Create Application

```sh
$ sbt aws::ebApplicationCreateOrUpdateAndWait
```

- Create Configuration Template

```sh
$ sbt aws::ebConfigurationTemplateCreateOrUpdate
```

- Create Application Version 

```sh
$ sbt aws::ebApplicationVersionCreateOrUpdateAndWait
```

- Create Environment(env parameter is Environment Name, It's a dynamic option that does not use the value(ebEnvironmentName) of the on build.sbt.)

```sh
$ sbt aws::ebEnvironmentCreateOrUpdateAndWait [env=staging]
```

- List Available Solution Stacks

```scala
aws::ebListAvailableSolutionStacks
```

## Auto Deploying

Add this to your build.sbt file.

```scala
ebDeploySettings
```

Execute auto deploying command.

```scala
$ sbt aws::ebDeploy
```

Following is aws::ebDeploy's automatic task process.

```
aws::ebEnvironmentCreateOrUpdateAndWait -> aws::ebApplicationVersionCreateOrUpdateAndWait -> aws::ebConfigurationTemplateCreateOrUpdate -> aws::ebApplicationCreateOrUpdateAndWait
```

## Appendix
      
[Play2 Application Integration by sbt-assembly](src/sbt-test/sbt-aws-eb/play-sample-scala-sbt-assembly)


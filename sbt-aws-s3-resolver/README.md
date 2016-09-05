# sbt-aws-s3-resolver

This SBT plugin adds support for using Amazon S3 for resolving and publishing using s3:// urls.

## Installation

Add this to your project/plugins.sbt file:

```scala
resolvers += "Sonatype OSS Release Repository" at "https://oss.sonatype.org/content/repositories/releases/"

addSbtPlugin("com.chatwork" % "sbt-aws-s3-resolver" % "0.0.24-SNAPSHOT")
```

## Usage

### Resolving Dependencies via S3

```scala
publishTo := {
  if (isSnapshot.value)
    Some(
      (s3Resolver in aws).value("yours Maven Snapshot Repository", "s3://backet-name/snapshots")
    )
  else
    Some(
      (s3Resolver in aws).value("yours Maven Release Repository", "s3://backet-name/releases")
    )
}
```

### Publishing to S3

```scala
resolvers ++= Seq[Resolver](
  (s3Resolver in aws).value("yours Maven Snapshot Repository", "s3://backet-name/snapshots"),
  (s3Resolver in aws).value("yours Maven Release Repository", "s3://backet-name/releases")
)
```

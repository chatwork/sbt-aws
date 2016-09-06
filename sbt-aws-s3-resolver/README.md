# sbt-aws-s3-resolver

This SBT plugin adds support for using Amazon S3 for resolving and publishing using s3:// urls.

## Installation

Add this to your project/plugins.sbt file:

```scala
resolvers += "Sonatype OSS Release Repository" at "https://oss.sonatype.org/content/repositories/releases/"

addSbtPlugin("com.chatwork" % "sbt-aws-s3-resolver" % "1.0.24")
```

## Usage

### Common Configurations

```scala
// Default S3 Region is Tokyo, Please set your region for S3.
s3Region in aws := com.amazonaws.services.s3.model.Region.AP_Tokyo

// Default Deploy Style is Maven, other is Ivy Style.
s3DeployStyle in aws := DeployStyle.Maven

// Default Server Side Encryption is false.
s3ServerSideEncryption in aws := false

// Default ACL is PublicRead, if you want to use the private repository, please set `CannedAccessControlList.Private` to `s3Acl in aws`
s3Acl in aws := com.amazonaws.services.s3.model.CannedAccessControlList.PublicRead

// Default overwrite option is isSnaphost.value(if it's `-SNAPSHOT.jar`, overwrite option is true)
s3OverwriteObject in aws := isSnapshot.value
```

### Resolving Dependencies via S3

```scala
publishTo := {
  if (isSnapshot.value)
    Some(
      (s3Resolver in aws).value("your Maven Snapshot Repository", "s3://backet-name/snapshots")
    )
  else
    Some(
      (s3Resolver in aws).value("your Maven Release Repository", "s3://backet-name/releases")
    )
}
```

### Publishing to S3

```scala
resolvers ++= Seq[Resolver](
  (s3Resolver in aws).value("your Maven Snapshot Repository", "s3://backet-name/snapshots"),
  (s3Resolver in aws).value("your Maven Release Repository", "s3://backet-name/releases")
)
```

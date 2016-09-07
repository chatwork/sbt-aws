# sbt-aws-s3-resolver

We imported good-parts of following sbt plugins, so we respect those.
- [sbt-s3-resolver](https://github.com/ohnosequences/sbt-s3-resolver)
- [fm-sbt-s3-resolver](https://github.com/frugalmechanic/fm-sbt-s3-resolver)

This SBT plugin adds support for using Amazon S3 for resolving and publishing using s3:// urls.

| _Ivy artifacts_ | publish | resolve | • | _Maven artifacts_ | publish | resolve |
|:---------------:|:-------:|:-------:|:-:|:-----------------:|:-------:|:-------:|
|   **public**    |    ✓    |    ✓    |   |    **public**     |    ✓    |    ✓    |
|   **private**   |    ✓    |    ✓    |   |    **private**    |    ✓    |    ✓    |

## Installation

Add this to your project/plugins.sbt file:

```scala
resolvers += "Sonatype OSS Release Repository" at "https://oss.sonatype.org/content/repositories/releases/"

addSbtPlugin("com.chatwork" % "sbt-aws-s3-resolver" % "1.0.28-SNAPSHOT")
```

## Usage

### Base Configurations

```scala
// Default S3 Region is Tokyo, Please set your region for S3.
s3Region in aws := com.amazonaws.services.s3.model.Region.AP_Tokyo

// Default Deploy Style is Maven, other is Ivy Style.
s3DeployStyle in aws := DeployStyle.Maven

// Default Server Side Encryption is false.
s3ServerSideEncryption in aws := false

// Default ACL is PublicRead, if you want to use the private repository, please set `CannedAccessControlList.Private` to `s3Acl in aws`
s3Acl in aws := com.amazonaws.services.s3.model.CannedAccessControlList.PublicRead

// Default overwrite option is isSnaphost.value(if your jar is `-SNAPSHOT.jar`, overwrite option is true)
s3OverwriteObject in aws := isSnapshot.value
```

### Resolving Dependencies via S3

```scala
// If maven style, publishMavenStyle is true.
publishMavenStyle := true

publishTo := {
  if (isSnapshot.value)
    Some(
      (s3Resolver in aws).value("your Maven Snapshot Repository", "s3://bucket-name/snapshots")
    )
  else
    Some(
      (s3Resolver in aws).value("your Maven Release Repository", "s3://bucket-name/releases")
    )
}
```

### Publishing to S3

```scala
resolvers ++= Seq[Resolver](
  (s3Resolver in aws).value("your Maven Snapshot Repository", "s3://bucket-name/snapshots"),
  (s3Resolver in aws).value("your Maven Release Repository", "s3://bucket-name/releases")
)
```

### IAM Policy Examples

Read/Write Policy (for publishing)

```javascript
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:GetBucketLocation"],
      "Resource": "arn:aws:s3:::*"
    },
    {
      "Effect": "Allow",
      "Action": ["s3:ListBucket"],
      "Resource": ["arn:aws:s3:::bucket-name"]
    },
    {
      "Effect": "Allow",
      "Action": ["s3:DeleteObject","s3:GetObject","s3:PutObject"],
      "Resource": ["arn:aws:s3:::backet-name/*"]
    }
  ]
}
```

Read-Only Policy

```javascript
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:GetBucketLocation"],
      "Resource": "arn:aws:s3:::*"
    },
    {
      "Effect": "Allow",
      "Action": ["s3:ListBucket"],
      "Resource": ["arn:aws:s3:::bucket-name"]
    },
    {
      "Effect": "Allow",
      "Action": ["s3:GetObject"],
      "Resource": ["arn:aws:s3:::bucket-name/*"]
    }
  ]
}
```




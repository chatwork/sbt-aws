package com.chatwork.sbt.aws.s3

import com.chatwork.sbt.aws.core.{SbtAwsCoreKeys, SbtAwsCorePlugin}
import com.chatwork.sbt.aws.s3.SbtAwsS3._
import sbt.{AutoPlugin, Def, Plugins}

object SbtAwsS3Plugin extends AutoPlugin {

  override def requires: Plugins = SbtAwsCorePlugin

  object autoImport extends SbtAwsS3Keys

  import SbtAwsCoreKeys._
  import SbtAwsS3Keys._

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    s3OverwriteObject in aws := {
      (config in aws).value.getBooleanValue(s3OverwriteObject.key.label).getOrElse(false)
    },
    s3CreateBucket in aws := {
      (config in aws).value.getBooleanValue(s3CreateBucket.key.label).getOrElse(false)
    },
    s3ObjectMetadata in aws := None,
    s3File := None,
    s3Key := "",
    s3BucketName := "",
    s3Upload in aws <<= s3UploadTask
  )
}

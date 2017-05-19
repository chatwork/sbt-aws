package com.chatwork.sbt.aws.s3

import com.amazonaws.services.s3.model.{ObjectMetadata, Region}
import sbt._
trait SbtAwsS3Keys {
  type S3ACL = com.amazonaws.services.s3.model.CannedAccessControlList

  lazy val s3Region = SettingKey[Region]("s3-region", "AWS Region for your S3 resolvers")

  lazy val s3BucketName = settingKey[String]("s3-bucket-name")

  lazy val s3Key = settingKey[String]("s3-key")

  lazy val s3File = settingKey[Option[File]]("s3-file")

  lazy val s3ObjectMetadata = settingKey[Option[ObjectMetadata]]("s3-object-metadata")

  lazy val s3OverwriteObject = settingKey[Boolean]("s3-overwrite-object")

  lazy val s3ServerSideEncryption = SettingKey[Boolean](
    "s3-server-side-encryption",
    "Controls whether publishing resolver will use server side encryption")

  lazy val s3Acl = SettingKey[S3ACL](
    "s3-acl",
    "Controls whether published artifacts are accessible publicly via http(s) or not")

  lazy val s3Upload = taskKey[String]("s3-upload")

  lazy val s3CreateBucket = settingKey[Boolean]("s3-create-bucket")

}

object SbtAwsS3Keys extends SbtAwsS3Keys

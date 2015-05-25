package com.chatwork.sbt.aws.eb

import com.amazonaws.services.elasticbeanstalk.model.{ ApplicationDescription, ApplicationVersionDescription }
import sbt._

trait SbtAwsEbKeys {

  lazy val ebBuildBundle = taskKey[File]("build-bundle")

  lazy val ebUploadBundle = taskKey[Unit]("upload-bundle")

  lazy val ebBundleTargetFiles = taskKey[Seq[(File, String)]]("zip-target-file")

  lazy val ebBundleFileName = settingKey[String]("zip-file")

  lazy val ebS3BucketName = settingKey[String]("eb-s3-bucket-name")

  lazy val ebS3KeyCreator = taskKey[String => String]("eb-s3-key-creator")

  lazy val ebS3CreateBucket = settingKey[Boolean]("eb-s3-create-bucket")

  lazy val ebApplicationName = settingKey[String]("eb-application-name")

  lazy val ebApplicationDescription = settingKey[Option[String]]("eb-application-desc")

  lazy val ebVersionLabel = settingKey[String]("eb-version-label")

  lazy val ebTemplateName = settingKey[String]("eb-template-name")

  lazy val ebTemplates = settingKey[Seq[EbConfigurationTemplate]]("eb-templates")

  lazy val ebCreateApplication = taskKey[ApplicationDescription]("create-application")

  lazy val ebCreateApplicationVersion = taskKey[ApplicationVersionDescription]("create-application-version")

}

object SbtAwsEbKeys extends SbtAwsEbKeys

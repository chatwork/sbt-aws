package com.chatwork.sbt.aws.eb

import com.amazonaws.services.elasticbeanstalk.model.{ ApplicationDescription, ApplicationVersionDescription }
import sbt._

trait SbtAwsEbKeys {

  lazy val ebBuildBundle = taskKey[File]("build-bundle")

  lazy val ebUploadBundle = taskKey[Unit]("upload-bundle")

  lazy val ebBundleTargetFiles = taskKey[Seq[(File, String)]]("zip-target-file")

  lazy val ebBundleFileName = settingKey[String]("zip-file")

  lazy val ebS3BucketName = settingKey[Option[String]]("eb-s3-bucket-name")

  lazy val ebS3KeyMapper = taskKey[String => String]("eb-s3-key-mapper")

  lazy val ebS3CreateBucket = settingKey[Boolean]("eb-s3-create-bucket")

  lazy val ebApplicationName = settingKey[String]("eb-application-name")

  lazy val ebApplicationDescription = settingKey[Option[String]]("eb-application-desc")

  lazy val ebVersionLabel = settingKey[String]("eb-version-label")

  lazy val ebTemplateName = settingKey[String]("eb-template-name")

  lazy val ebTemplates = settingKey[Seq[EbConfigurationTemplate]]("eb-templates")

  // ---

  lazy val ebApplicationCreate = taskKey[ApplicationDescription]("create-application")

  lazy val ebApplicationCreateAndWait = taskKey[ApplicationDescription]("create-application-and-wait")

  lazy val ebApplicationUpdate = taskKey[ApplicationDescription]("update-application")

  lazy val ebApplicationUpdateAndWait = taskKey[ApplicationDescription]("update-application")

  lazy val ebApplicationDelete = taskKey[Unit]("delete-application")

  lazy val ebApplicationDeleteAndWait = taskKey[Unit]("delete-application")

  lazy val ebApplicationCreateOrUpdate = taskKey[ApplicationDescription]("create-or-update-application")

  lazy val ebApplicationCreateOrUpdateAndWait = taskKey[ApplicationDescription]("create-or-update-application")

  // ---

  lazy val ebCreateApplicationVersion = taskKey[ApplicationVersionDescription]("create-application-version")

  lazy val ebUpdateApplicationVersion = taskKey[ApplicationVersionDescription]("update-application-version")

  lazy val ebDeleteApplicationVersion = taskKey[ApplicationVersionDescription]("delete-application-version")

  lazy val ebCreateOrUpdateApplicationVersion = taskKey[ApplicationVersionDescription]("create-or-update-application-version")

  // ---

  lazy val ebCreateConfigurationTemplate = taskKey[ApplicationVersionDescription]("create-configuration-template")

}

object SbtAwsEbKeys extends SbtAwsEbKeys

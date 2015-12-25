package com.chatwork.sbt.aws.eb

import com.amazonaws.services.elasticbeanstalk.model._
import com.chatwork.sbt.aws.eb.SbtAwsEbPlugin.autoImport
import sbt._

trait SbtAwsEbKeys {

  import autoImport._

  lazy val ebBuildBundle = taskKey[File]("build-bundle")

  lazy val ebUploadBundle = taskKey[S3Location]("upload-bundle")

  lazy val ebBundleTargetFiles = taskKey[Seq[(File, String)]]("zip-target-file")

  lazy val ebBundleFileName = settingKey[String]("zip-file")

  lazy val ebS3BucketName = settingKey[Option[String]]("eb-s3-bucket-name")

  lazy val ebS3KeyMapper = taskKey[String => String]("eb-s3-key-mapper")

  lazy val ebS3CreateBucket = settingKey[Boolean]("eb-s3-create-bucket")

  lazy val ebApplicationName = settingKey[String]("eb-application-name")

  lazy val ebApplicationDescription = settingKey[Option[String]]("eb-application-desc")

  lazy val ebApplicationVersionLabel = taskKey[String]("eb-application-version-label")

  lazy val ebApplicationVersionDescription = settingKey[Option[String]]("eb-application-version-desc")

  lazy val ebTemplateName = settingKey[String]("eb-template-name")

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

  lazy val ebUseBundle = settingKey[Boolean]("use-bundle")

  lazy val ebAutoCreateApplication = settingKey[Option[Boolean]]("auto-create-application")

  lazy val ebApplicationVersionCreate = taskKey[ApplicationVersionDescription]("create-application-version")

  lazy val ebApplicationVersionCreateAndWait = taskKey[ApplicationVersionDescription]("create-application-version-and-wait")

  lazy val ebApplicationVersionUpdate = taskKey[ApplicationVersionDescription]("update-application-version")

  lazy val ebApplicationVersionUpdateAndWait = taskKey[ApplicationVersionDescription]("update-application-version-and-wait")

  lazy val ebApplicationVersionDelete = taskKey[Unit]("delete-application-version")

  lazy val ebApplicationVersionDeleteAndWait = taskKey[Unit]("delete-application-version")

  lazy val ebApplicationVersionCreateOrUpdate = taskKey[ApplicationVersionDescription]("create-or-update-application-version")

  lazy val ebApplicationVersionCreateOrUpdateAndWait = taskKey[ApplicationVersionDescription]("create-or-update-application-version-and-wait")

  // ---

  lazy val ebEnvironmentName = settingKey[String]("environment-name")

  lazy val ebEnvironmentDescription = settingKey[Option[String]]("environment-description")

  lazy val ebEnvironmentUseVersionLabel = taskKey[Option[String]]("environment-use-version-label")

  lazy val ebSolutionStackName = settingKey[Option[String]]("soulution-stack-name")

  lazy val ebTags = settingKey[Seq[EbTag]]("tags")

  lazy val ebConfigurationTemplateName = settingKey[Option[String]]("configuration-template-name")

  lazy val ebConfigurationOptionSettings = settingKey[Seq[EbConfigurationOptionSetting]]("configuration-option-settings")

  lazy val ebOptionSpecifications = settingKey[Seq[EbOptionSpecification]]("option-specification")

  lazy val ebEnvironmentTier = settingKey[Option[EbEnvironmentTier]]("environment-tier")

  lazy val ebCNAMEPrefix = settingKey[Option[String]]("cname-prefix")

  lazy val ebEnvironmentCreate = inputKey[EnvironmentDescription]("create-environment")

  lazy val ebEnvironmentCreateAndWait = inputKey[EnvironmentDescription]("create-environment-and-wait")

  lazy val ebEnvironmentUpdate = inputKey[EnvironmentDescription]("update-environment")

  lazy val ebEnvironmentUpdateAndWait = inputKey[EnvironmentDescription]("update-environment-and-wait")

  lazy val ebEnvironmentCreateOrUpdate = inputKey[EnvironmentDescription]("create-or-update-environment")

  lazy val ebEnvironmentCreateOrUpdateAndWait = inputKey[EnvironmentDescription]("create-or-update-environment-and-wait")

  // ---

  lazy val ebConfigurationTemplate = settingKey[Option[EbConfigurationTemplate]]("eb-configuration-template")

  lazy val ebConfigurationTemplateCreate = taskKey[CreateConfigurationTemplateResult]("create-configuration-template")

  lazy val ebConfigurationTemplateUpdate = taskKey[UpdateConfigurationTemplateResult]("update-configuration-template")

  lazy val ebConfigurationTemplateDelete = taskKey[Unit]("update-configuration-template")

}

object SbtAwsEbKeys extends SbtAwsEbKeys

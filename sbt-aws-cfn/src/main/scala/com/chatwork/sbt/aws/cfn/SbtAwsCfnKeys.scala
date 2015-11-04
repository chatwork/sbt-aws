package com.chatwork.sbt.aws.cfn

import com.amazonaws.services.cloudformation.model.Stack
import sbt._

trait SbtAwsCfnKeys {

  type Parameters = Map[String, String]
  type Tags = Map[String, String]

  val cfnTemplatesSourceFolder = settingKey[File]("cfn-template-source-folder")
  val cfnTemplates = settingKey[Seq[File]]("cfn-templates")

  val cfnStackParams = taskKey[Parameters]("cfn-stack-params")
  val cfnStackTags = settingKey[Tags]("cfn-stack-tags")
  val cfnStackCapabilities = settingKey[Seq[String]]("cfn-stack-capabilities")
  val cfnStackRegion = settingKey[String]("cfn-stack-region")

  val cfnArtifactId = settingKey[String]("cfn-artifacti-id")
  val cfnVersion = settingKey[String]("cfn-version")
  val cfnStackName = settingKey[Option[String]]("cfn-stack-name")
  val cfnS3BucketName = settingKey[Option[String]]("cfn-s3-bucket-name")
  val cfnS3KeyMapper = settingKey[String => String]("cfn-s3-key-functor")
  val cfnCapabilityIam = settingKey[Boolean]("cfn-capability-iam")

  val cfnUploadTemplate = taskKey[URL]("cfn-upload-template")

  // stack operations
  val cfnStackValidateOnFile = taskKey[Seq[File]]("cfn-validate-templates-on-file")
  val cfnStackValidateOnURL = taskKey[URL]("cfn-validate-templates-on-url")

  val cfnStackStatus = taskKey[Option[String]]("cfn-stack-status")
  val cfnStackWait = taskKey[Option[String]]("cfn-stack-wait")

  val cfnStackDescribe = taskKey[Option[Stack]]("cfn-stack-describe")

  val cfnStackCreate = taskKey[Option[String]]("cfn-stack-create")
  val cfnStackCreateAndWait = taskKey[Option[String]]("cfn-stack-create-wait")

  val cfnStackUpdate = taskKey[Option[String]]("cfn-stack-update")
  val cfnStackUpdateAndWait = taskKey[Option[String]]("cfn-stack-update-wait")

  val cfnStackDelete = taskKey[Option[String]]("cfn-stack-delete")
  val cfnStackDeleteAndWait = taskKey[Option[String]]("cfn-stack-delete-wait")

  val cfnStackCreateOrUpdate = taskKey[Option[String]]("cfn-stack-create-or-update")
  val cfnStackCreateOrUpdateAndWait = taskKey[Option[String]]("cfn-stack-create-or-update-wait")

}

object SbtAwsCfnKeys extends SbtAwsCfnKeys
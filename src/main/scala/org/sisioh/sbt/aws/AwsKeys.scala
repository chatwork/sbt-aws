package org.sisioh.sbt.aws

import com.amazonaws.regions.Regions
import com.amazonaws.services.cloudformation.model.Stack
import com.amazonaws.services.elasticbeanstalk.model.{ ApplicationDescription, ApplicationVersionDescription, ConfigurationOptionSetting, OptionSpecification }
import com.amazonaws.services.s3.model.ObjectMetadata
import org.sisioh.config.{ Configuration => SisiohConfiguration }
import sbt._

trait AwsKeys {

  lazy val aws = taskKey[Unit]("aws")

  lazy val region = settingKey[Regions]("region")

  lazy val credentialProfileName = settingKey[String]("credential-profile-name")

  lazy val environmentName = settingKey[String]("env")

  lazy val configFile = settingKey[File]("configFile")

  lazy val config = settingKey[SisiohConfiguration]("config")

  lazy val poolingInterval = settingKey[Int]("pooling-interval")

}

trait EBKeys {

  lazy val ebApplicationName = settingKey[String]("eb-application-name")

  lazy val ebApplicationDescription = settingKey[Option[String]]("eb-application-desc")

  lazy val ebVersionLabel = settingKey[String]("eb-version-label")

  lazy val ebTemplateName = settingKey[String]("eb-template-name")

  lazy val ebTemplates = settingKey[Seq[EbConfigurationTemplate]]("eb-templates")

  lazy val ebCreateApplication = taskKey[ApplicationDescription]("create-application")

  lazy val ebCreateApplicationVersion = taskKey[ApplicationVersionDescription]("create-application-version")

}

trait CfnKeys {

  type Parameters = Map[String, String]
  type Tags = Map[String, String]

  val cfnTemplatesSourceFolder = settingKey[File]("cfn-template-source-folder")
  val cfnTemplates = settingKey[Seq[File]]("cfn-templates")

  val cfnStackTemplate = taskKey[String]("cfn-stack-template")
  val cfnStackParams = taskKey[Parameters]("cfn-stack-params")
  val cfnStackTags = settingKey[Tags]("cfn-stack-tags")
  val cfnStackCapabilities = settingKey[Seq[String]]("cfn-stack-capabilities")
  val cfnStackRegion = settingKey[String]("cfn-stack-region")
  val cfnStackName = settingKey[String]("cfn-stack-name")

  // stack operations
  val cfnStackValidate = taskKey[Seq[File]]("cfn-validate-templates")
  val cfnStackStatus = taskKey[Option[String]]("cfn-stack-status")
  val cfnStackWait = taskKey[Option[String]]("cfn-stack-wait")

  val cfnStackDescribe = taskKey[Option[Stack]]("cfn-stack-describe")

  val cfnStackCreate = taskKey[String]("cfn-stack-create")
  val cfnStackCreateAndWait = taskKey[Option[String]]("cfn-stack-create-wait")

  val cfnStackUpdate = taskKey[Option[String]]("cfn-stack-update")
  val cfnStackUpdateAndWait = taskKey[Option[String]]("cfn-stack-update-wait")

  val cfnStackDelete = taskKey[Unit]("cfn-stack-delete")
  val cfnStackDeleteAndWait = taskKey[Option[String]]("cfn-stack-delete-wait")

}

trait S3Keys {

  lazy val s3BucketName = settingKey[String]("s3-bucket-name")

  lazy val s3Key = settingKey[String]("s3-key")

  lazy val s3File = settingKey[Option[File]]("s3-file")

  lazy val s3ObjectMetadata = settingKey[Option[ObjectMetadata]]("s3-object-metadata")

  lazy val s3OverwriteObject = settingKey[Boolean]("s3-overwrite-object")

  lazy val s3Upload = taskKey[Option[String]]("s3-upload")

  lazy val s3CreateBucket = settingKey[Boolean]("s3-create-bucket")

}

object AwsKeys extends AwsKeys {

  object EBKeys extends EBKeys

  object S3Keys extends S3Keys

  object CfnKeys extends CfnKeys

}

case class EbConfigurationTemplate(name: String, description: String,
                                   solutionStackName: String,
                                   optionSettings: Seq[ConfigurationOptionSetting],
                                   optionsToRemoves: Seq[OptionSpecification],
                                   recreate: Boolean)

package org.sisioh.sbt.aws

import com.amazonaws.regions.Regions
import com.amazonaws.services.cloudformation.model.Stack
import com.amazonaws.services.elasticbeanstalk.model.{ ApplicationDescription, ApplicationVersionDescription }
import com.amazonaws.services.s3.model.ObjectMetadata
import org.sisioh.config.{ Configuration => SisiohConfiguration }
import org.sisioh.sbt.aws.SbtAws._
import sbt.Keys._
import sbt._

object SbtAwsPlugin extends AutoPlugin {

  override def trigger = allRequirements

  trait AwsKeys {
    lazy val aws = taskKey[Unit]("aws")

    lazy val region = settingKey[Regions]("region")

    lazy val credentialProfileName = settingKey[Option[String]]("credential-profile-name")

    lazy val environmentName = settingKey[String]("env")

    lazy val configFileFolder = settingKey[File]("config-folder")

    lazy val configFile = settingKey[File]("config-file")

    lazy val config = settingKey[SisiohConfiguration]("config")

    lazy val awsConfig = settingKey[SisiohConfiguration]("aws-config")

    lazy val poolingInterval = settingKey[Int]("pooling-interval")

    // ---

    lazy val ebApplicationName = settingKey[String]("eb-application-name")

    lazy val ebApplicationDescription = settingKey[Option[String]]("eb-application-desc")

    lazy val ebVersionLabel = settingKey[String]("eb-version-label")

    lazy val ebTemplateName = settingKey[String]("eb-template-name")

    lazy val ebTemplates = settingKey[Seq[EbConfigurationTemplate]]("eb-templates")

    lazy val ebCreateApplication = taskKey[ApplicationDescription]("create-application")

    lazy val ebCreateApplicationVersion = taskKey[ApplicationVersionDescription]("create-application-version")

    // ---

    type Parameters = Map[String, String]
    type Tags = Map[String, String]

    val cfnTemplatesSourceFolder = settingKey[File]("cfn-template-source-folder")
    val cfnTemplates = settingKey[Seq[File]]("cfn-templates")

    val cfnStackTemplate = taskKey[String]("cfn-stack-template")
    val cfnStackParams = taskKey[Parameters]("cfn-stack-params")
    val cfnStackTags = settingKey[Tags]("cfn-stack-tags")
    val cfnStackCapabilities = settingKey[Seq[String]]("cfn-stack-capabilities")
    val cfnStackRegion = settingKey[String]("cfn-stack-region")
    val cfnStackName = settingKey[Option[String]]("cfn-stack-name")

    // stack operations
    val cfnStackValidate = taskKey[Seq[File]]("cfn-validate-templates")
    val cfnStackStatus = taskKey[Option[String]]("cfn-stack-status")
    val cfnStackWait = taskKey[Option[String]]("cfn-stack-wait")

    val cfnStackDescribe = taskKey[Option[Stack]]("cfn-stack-describe")

    val cfnStackCreate = taskKey[Option[String]]("cfn-stack-create")
    val cfnStackCreateAndWait = taskKey[Option[String]]("cfn-stack-create-wait")

    val cfnStackUpdate = taskKey[Option[String]]("cfn-stack-update")
    val cfnStackUpdateAndWait = taskKey[Option[String]]("cfn-stack-update-wait")

    val cfnStackDelete = taskKey[Unit]("cfn-stack-delete")
    val cfnStackDeleteAndWait = taskKey[Option[String]]("cfn-stack-delete-wait")

    val cfnStackCreateOrUpdateAndWait = taskKey[Option[String]]("cfn-stack-create-or-update-wait")

    // ---

    lazy val s3BucketName = settingKey[String]("s3-bucket-name")

    lazy val s3Key = settingKey[String]("s3-key")

    lazy val s3File = settingKey[Option[File]]("s3-file")

    lazy val s3ObjectMetadata = settingKey[Option[ObjectMetadata]]("s3-object-metadata")

    lazy val s3OverwriteObject = settingKey[Boolean]("s3-overwrite-object")

    lazy val s3Upload = taskKey[Option[String]]("s3-upload")

    lazy val s3CreateBucket = settingKey[Boolean]("s3-create-bucket")
  }

  object AwsKeys extends AwsKeys

  object autoImport extends AwsKeys

  import AwsKeys._

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    credentialProfileName in aws := None,
    region in aws := Regions.AP_NORTHEAST_1,
    environmentName in aws := System.getProperty("sbt.aws.profile", "dev"),
    configFileFolder in aws := file("env"),
    configFile in aws := {
      val parent = (configFileFolder in aws).value
      val file = parent / ((environmentName in aws).value + ".conf")
      file
    },
    config in aws := {
      Option(SisiohConfiguration.parseFile((configFile in aws).value)).getOrElse(SisiohConfiguration.empty)
    },
    awsConfig in aws := {
      (config in aws).value.getConfiguration(aws.key.label).getOrElse(SisiohConfiguration.empty)
    },
    poolingInterval in aws := 1000,
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
    s3Upload in aws <<= s3UploadTask,
    ebApplicationName in aws := "",
    ebApplicationDescription in aws := None,
    ebVersionLabel in aws := "1.0.0-SNAPSHOT",
    ebCreateApplication in aws <<= ebCreateApplicationTask,
    ebCreateApplicationVersion in aws <<= ebCreateApplicationVersionTask,
    cfnTemplatesSourceFolder in aws <<= baseDirectory {
      base => base / "aws/cfn/templates"
    },
    cfnTemplates in aws := {
      val templates = (cfnTemplatesSourceFolder in aws).value ** GlobFilter("*.template")
      templates.get
    },
    cfnStackTemplate in aws <<= stackTemplatesTask,
    cfnStackParams in aws := {
      (awsConfig in aws).value.getConfiguration(cfnStackParams.key.label)
        .map(_.entrySet.map { case (k, v) => (k, v.unwrapped().toString) }.toMap).getOrElse(Map.empty)
    },
    cfnStackTags in aws := {
      (awsConfig in aws).value.getConfiguration(cfnStackTags.key.label)
        .map(_.entrySet.map { case (k, v) => (k, v.unwrapped().toString) }.toMap).getOrElse(Map.empty)
    },
    cfnStackCapabilities in aws := {
      (awsConfig in aws).value
        .getStringValues(cfnStackCapabilities.key.label)
        .getOrElse(Seq.empty)
    },
    cfnStackRegion in aws := "",
    cfnStackName in aws := {
      (awsConfig in aws).value.getStringValue(cfnStackName.key.label)
    },
    // ---
    cfnStackValidate in aws <<= stackValidateTask(),
    cfnStackDescribe in aws <<= describeStacksTask().map(s => s.headOption),
    cfnStackStatus in aws <<= statusStackTask(),
    cfnStackCreate in aws <<= createStackTask(),
    cfnStackCreateAndWait in aws <<= createStackAndWaitTask(),
    cfnStackUpdate in aws <<= updateStackTask(),
    cfnStackUpdateAndWait in aws <<= updateStackAndWaitTask(),
    cfnStackDelete in aws <<= deleteStackTask(),
    cfnStackDeleteAndWait in aws <<= deleteStackAndWaitTask(),
    cfnStackWait in aws <<= waitStackTask(),
    cfnStackCreateOrUpdateAndWait in aws <<= createOrUpdateStackTask(),
    watchSources <++= (cfnTemplates in aws) map identity
  )

}


package com.chatwork.sbt.aws.cfn

import com.chatwork.sbt.aws.cfn.SbtAwsCfn._
import com.chatwork.sbt.aws.core.SbtAwsCoreKeys
import com.chatwork.sbt.aws.s3.SbtAwsS3Plugin
import sbt.Keys._
import sbt._

object SbtAwsCfnPlugin extends AutoPlugin with SbtPluginHelper {

  override def trigger = allRequirements

  override def requires: Plugins = SbtAwsS3Plugin

  object autoImport extends SbtAwsCfnKeys

  import SbtAwsCfnKeys._
  import SbtAwsCoreKeys._

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    cfnTemplatesSourceFolder in aws := baseDirectory { base =>
      base / defaultTemplateDirectory
    }.value,
    cfnTemplates in aws := {
      val templates = (cfnTemplatesSourceFolder in aws).value ** GlobFilter("*.template")
      templates.get
    },
    cfnArtifactId := {
      getConfigValue(classOf[String],
                     (awsConfig in aws).value,
                     cfnArtifactId.key.label,
                     (name in ThisProject).value)
    },
    cfnVersion := {
      getConfigValue(classOf[String],
                     (awsConfig in aws).value,
                     cfnVersion.key.label,
                     (version in ThisProject).value)
    },
    cfnS3BucketName in aws := {
      getConfigValueOpt(classOf[String], (awsConfig in aws).value, cfnS3BucketName.key.label)
    },
    cfnS3KeyMapper in aws := identity,
    cfnStackParams in aws := {
      getConfigValuesAsMap((awsConfig in aws).value, cfnStackParams.key.label)
    },
    cfnStackTags in aws := {
      getConfigValuesAsMap((awsConfig in aws).value, cfnStackTags.key.label)
    },
    cfnStackCapabilities in aws := {
      getConfigValuesAsSeq(classOf[String],
                           (awsConfig in aws).value,
                           cfnStackCapabilities.key.label,
                           Seq.empty)
    },
    cfnStackRegion in aws := "",
    cfnStackName in aws := {
      getConfigValueOpt(classOf[String], (awsConfig in aws).value, cfnStackName.key.label)
    },
    cfnCapabilityIam in aws := {
      getConfigValue(classOf[Boolean], (awsConfig in aws).value, cfnCapabilityIam.key.label, false)
    },
    // ---
    cfnUploadTemplate in aws := uploadTemplateFileTask().value,
    cfnValidateTemplateOnS3 in aws := stackValidateOnFileTask().value,
    cfnValidateTemplateOnURL in aws := stackValidateOnURLTask().value,
    cfnStackDescribe in aws := describeStacksTask().map(s => s.headOption).value,
    cfnStackStatus in aws := statusStackTask().value,
    cfnStackCreate in aws := createStackTask().value,
    cfnStackCreateAndWait in aws := createStackAndWaitTask().value,
    cfnStackUpdate in aws := updateStackTask().value,
    cfnStackUpdateAndWait in aws := updateStackAndWaitTask().value,
    cfnStackDelete in aws := deleteStackTask().value,
    cfnStackDeleteAndWait in aws := deleteStackAndWaitTask().value,
    cfnStackWait in aws := waitStackTask().value,
    cfnStackCreateOrUpdate in aws := createOrUpdateStackTask().value,
    cfnStackCreateOrUpdateAndWait in aws := createOrUpdateStackAndWaitTask().value,
    watchSources ++= (cfnTemplates in aws).value
  )

}

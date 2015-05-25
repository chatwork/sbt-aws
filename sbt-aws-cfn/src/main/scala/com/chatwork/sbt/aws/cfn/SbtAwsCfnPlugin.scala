package com.chatwork.sbt.aws.cfn

import com.chatwork.sbt.aws.cfn.SbtAwsCfn._
import com.chatwork.sbt.aws.core.SbtAwsCoreKeys
import com.chatwork.sbt.aws.s3.SbtAwsS3Plugin
import sbt.Keys._
import sbt._

object SbtAwsCfnPlugin extends AutoPlugin {

  override def trigger = allRequirements

  override def requires: Plugins = SbtAwsS3Plugin

  object autoImport extends SbtAwsCfnKeys

  import SbtAwsCfnKeys._
  import SbtAwsCoreKeys._

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    cfnTemplatesSourceFolder in aws <<= baseDirectory {
      base => base / defaultTemplateDirectory
    },
    cfnTemplates in aws := {
      val templates = (cfnTemplatesSourceFolder in aws).value ** GlobFilter("*.template")
      templates.get
    },
    cfnArtifactId := {
      (awsConfig in aws).value.getStringValue(cfnArtifactId.key.label).getOrElse((name in ThisProject).value)
    },
    cfnVersion := {
      (awsConfig in aws).value.getStringValue(cfnVersion.key.label).getOrElse((version in ThisProject).value)
    },
    cfnS3BucketName in aws := {
      (awsConfig in aws).value.getStringValue(cfnS3BucketName.key.label).getOrElse("cfn-template")
    },
    cfnS3KeyCreator in aws := identity,
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
    cfnCapabilityIam in aws := {
      (awsConfig in aws).value.getBooleanValue(cfnCapabilityIam.key.label).getOrElse(false)
    },
    // ---
    cfnUploadTemplate in aws <<= uploadTemplateFileTask(),
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
    cfnStackCreateOrUpdate in aws <<= createOrUpdateStackTask(),
    cfnStackCreateOrUpdateAndWait in aws <<= createOrUpdateStackAndWaitTask(),
    watchSources <++= (cfnTemplates in aws) map identity
  )

}

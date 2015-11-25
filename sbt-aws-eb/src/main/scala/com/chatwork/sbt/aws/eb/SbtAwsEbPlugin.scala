package com.chatwork.sbt.aws.eb

import com.chatwork.sbt.aws.core.SbtAwsCoreKeys
import com.chatwork.sbt.aws.eb.SbtAwsEb._
import com.chatwork.sbt.aws.s3.SbtAwsS3Plugin
import sbt.Keys._
import sbt._

object SbtAwsEbPlugin extends AutoPlugin {

  override def trigger = allRequirements

  override def requires: Plugins = SbtAwsS3Plugin

  object autoImport extends SbtAwsEbKeys

  import SbtAwsCoreKeys._
  import SbtAwsEbKeys._

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    ebBundleTargetFiles in aws := Seq.empty,
    ebBundleFileName in aws := (name in thisProjectRef).value + "-bundle.zip",
    ebS3BucketName in aws := None,
    ebS3KeyMapper in aws := identity,
    ebApplicationName in aws := (name in thisProjectRef).value,
    ebApplicationDescription in aws := None,
    ebApplicationVersionLabel in aws := (version in thisProjectRef).value,
    ebApplicationVersionDescription in aws := None,
    ebS3CreateBucket in aws := false,
    // ---
    ebBuildBundle in aws <<= ebBuildBundleTask(),
    ebUploadBundle in aws <<= ebUploadBundleTask(),
    // ---
    ebApplicationCreate in aws <<= ebCreateApplicationTask(),
    ebApplicationCreateAndWait in aws <<= ebCreateApplicationAndWaitTask(),
    ebApplicationUpdate in aws <<= ebUpdateApplicationTask(),
    ebApplicationUpdateAndWait in aws <<= ebUpdateApplicationAndWaitTask(),
    ebApplicationDelete in aws <<= ebDeleteApplicationTask(),
    ebApplicationDeleteAndWait in aws <<= ebDeleteApplicationAndWaitTask(),
    ebApplicationCreateOrUpdate in aws <<= ebCreateOrUpdateApplicationTask(),
    ebApplicationCreateOrUpdateAndWait in aws <<= ebCreateOrUpdateApplicationTask(),
    // ---
    ebUseBundle in aws := true,
    ebAutoCreateApplication in aws := None,
    ebEnvironmentName in aws := (name in thisProjectRef).value + "-env",
    ebApplicationVersionCreate in aws <<= ebCreateApplicationVersionTask,
    ebApplicationVersionCreateAndWait in aws <<= ebCreateApplicationVersionAndWaitTask,
    ebApplicationVersionUpdate in aws <<= ebUpdateApplicationVersionTask,
    ebApplicationVersionUpdateAndWait in aws <<= ebUpdateApplicationVersionAndWaitTask,
    ebApplicationVersionDelete in aws <<= ebDeleteApplicationVersionTask,
    ebApplicationVersionDeleteAndWait in aws <<= ebDeleteApplicationVersionAndWaitTask,
    // ---
    ebEnvironmentCreate in aws <<= ebCreateEnvironmentTask,
    ebEnvironmentCreateAndWait in aws <<= ebCreateEnvironmentAndWaitTask,
    ebEnvironmentUpdate in aws <<= ebUpdateEnvironmentTask,
    ebEnvironmentUpdateAndWait in aws <<= ebUpdateEnvironmentAndWaitTask,
    ebEnvironmentCreateOrUpdate in aws <<= ebCreateOrUpdateEnvironmentTask,
    ebEnvironmentCreateOrUpdateAndWait in aws <<= ebCreateOrUpdateEnvironmentAndWaitTask,
    ebEnvironmentUseVersionLabel in aws := None,
    ebEnvironmentDescription in aws := None,
    ebSolutionStackName in aws := None,
    ebEnvironmentTier in aws := None,
    ebConfigurationTemplateName in aws := None,
    ebConfigurationOptionSettings in aws := Seq.empty,
    ebOptionSpecifications in aws := Seq.empty,
    ebTags in aws := Seq.empty,
    ebCNAMEPrefix in aws := None,
    // ---
    ebConfigurationTemplate in aws := None,
    ebConfigurationTemplateCreate in aws <<= ebCreateConfigurationTemplateTask(),
    ebConfigurationTemplateUpdate in aws <<= ebUpdateConfigurationTemplateTask(),
    ebConfigurationTemplateDelete in aws <<= ebDeleteConfigurationTemplateTask()

  )

}

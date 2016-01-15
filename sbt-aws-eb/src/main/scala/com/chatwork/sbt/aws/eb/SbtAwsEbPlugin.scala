package com.chatwork.sbt.aws.eb

import com.chatwork.sbt.aws.core.SbtAwsCoreKeys
import com.chatwork.sbt.aws.eb.SbtAwsEb._
import com.chatwork.sbt.aws.s3.SbtAwsS3Plugin
import sbt.Keys._
import sbt._

object SbtAwsEbPlugin extends AutoPlugin {

  override def trigger = allRequirements

  override def requires: Plugins = SbtAwsS3Plugin

  object autoImport extends SbtAwsEbKeys with Models {
  }

  import SbtAwsCoreKeys._
  import SbtAwsEbKeys._

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    // ---
    ebBundleDirectory in aws := file("ebBundle"),
    ebBundleContext in aws := Map(
      "name" -> (name in thisProjectRef).value,
      "version" -> (version in thisProjectRef).value
    ),
    ebTargetTemplates in aws := Map.empty,
    ebBundleTargetFiles in aws := Seq.empty,
    ebBundleFileName in aws := (name in thisProjectRef).value + "-bundle.zip",
    ebS3CreateBucket in aws := false,
    ebS3BucketName in aws := None,
    ebS3KeyMapper in aws := identity,
    ebGenerateFilesInBundle in aws <<= ebGenerateFilesInBundleTask(),
    ebBuildBundle in aws <<= ebBuildBundleTask(),
    ebUploadBundle in aws <<= ebUploadBundleTask(),
    // ---
    ebApplicationName in aws := (name in thisProjectRef).value,
    ebApplicationDescription in aws := None,
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
    ebApplicationVersionLabel in aws := (version in thisProjectRef).value + "_" + timestamp,
    ebApplicationVersionDescription in aws := None,
    ebApplicationVersionCreate in aws <<= ebCreateApplicationVersionTask(),
    ebApplicationVersionCreateAndWait in aws <<= ebCreateApplicationVersionAndWaitTask(),
    ebApplicationVersionUpdate in aws <<= ebUpdateApplicationVersionTask(),
    ebApplicationVersionUpdateAndWait in aws <<= ebUpdateApplicationVersionAndWaitTask(),
    ebApplicationVersionDelete in aws <<= ebDeleteApplicationVersionTask(),
    ebApplicationVersionDeleteAndWait in aws <<= ebDeleteApplicationVersionAndWaitTask(),
    ebApplicationVersionCreateOrUpdate in aws <<= ebCreateOrUpdateApplicationVersionTask(),
    ebApplicationVersionCreateOrUpdateAndWait in aws <<= ebCreateOrUpdateApplicationVersionAndWaitTask(),
    // ---
    ebEnvironmentUseVersionLabel in aws := Some((ebApplicationVersionLabel in aws).value),
    ebEnvironmentDescription in aws := None,
    ebSolutionStackName in aws := None,
    ebEnvironmentTier in aws := None,
    ebConfigurationTemplateName in aws := None,
    ebConfigurationOptionSettings in aws := Seq.empty,
    ebOptionSpecifications in aws := Seq.empty,
    ebTags in aws := Seq.empty,
    ebCNAMEPrefix in aws := None,
    ebEnvironmentCreate in aws <<= ebCreateEnvironmentTask(),
    ebEnvironmentCreateAndWait in aws <<= ebCreateEnvironmentAndWaitTask(),
    ebEnvironmentUpdate in aws <<= ebUpdateEnvironmentTask(),
    ebEnvironmentUpdateAndWait in aws <<= ebUpdateEnvironmentAndWaitTask(),
    ebEnvironmentCreateOrUpdate in aws <<= ebCreateOrUpdateEnvironmentTask(),
    ebEnvironmentCreateOrUpdateAndWait in aws <<= ebCreateOrUpdateEnvironmentAndWaitTask(),
    ebRestartAppServer in aws <<= ebRestartAppServerTask(),
    // ---
    ebConfigurationTemplate in aws := None,
    ebConfigurationTemplateCreate in aws <<= ebCreateConfigurationTemplateTask(),
    ebConfigurationTemplateUpdate in aws <<= ebUpdateConfigurationTemplateTask(),
    ebConfigurationTemplateCreateOrUpdate in aws <<= ebCreateOrUpdateConfigurationTemplateTask(),
    ebConfigurationTemplateDelete in aws <<= ebDeleteConfigurationTemplateTask(),
    // ---
    ebListAvailableSolutionStacks in aws <<= ebListAvailableSolutionStacksTask(),
    ebBuildBundle in aws <<= (ebBuildBundle in aws) dependsOn(ebGenerateFilesInBundle in aws)
  )

}

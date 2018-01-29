package com.chatwork.sbt.aws.eb

import com.chatwork.sbt.aws.core.SbtAwsCoreKeys
import com.chatwork.sbt.aws.eb.SbtAwsEb._
import com.chatwork.sbt.aws.s3.SbtAwsS3Plugin
import sbt.Keys._
import sbt._

object SbtAwsEbPlugin extends AutoPlugin {

  override def trigger = allRequirements

  override def requires: Plugins = SbtAwsS3Plugin

  import SbtAwsCoreKeys._
  import SbtAwsEbKeys._

  object autoImport extends SbtAwsEbKeys with Models {
    lazy val ebDeploySettings: Seq[Def.Setting[_]] = Seq(
      // ---
      ebConfigurationTemplateCreateOrUpdate in aws := (ebConfigurationTemplateCreateOrUpdate in aws).dependsOn(ebApplicationCreateOrUpdateAndWait in aws).value,
      ebApplicationVersionCreateOrUpdateAndWait in aws := (ebApplicationVersionCreateOrUpdateAndWait in aws).dependsOn(ebConfigurationTemplateCreateOrUpdate in aws).value,
      ebEnvironmentCreateOrUpdateAndWait in aws := (ebEnvironmentCreateOrUpdateAndWait in aws).dependsOn(ebApplicationVersionCreateOrUpdateAndWait in aws).evaluated,
      ebDeploy in aws := (ebEnvironmentCreateOrUpdateAndWait in aws).evaluated
    )
  }

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    // ---
    ebBundleDirectory in aws := (baseDirectory in thisProjectRef).value / "ebBundle",
    ebBundleContext in aws := Map(
      "name"    -> (name in thisProjectRef).value,
      "version" -> (version in thisProjectRef).value
    ),
    ebTargetTemplates in aws := Map.empty,
    ebBundleTargetFiles in aws := Seq.empty,
    ebBundleFileName in aws := (name in thisProjectRef).value + "-bundle.zip",
    ebS3CreateBucket in aws := false,
    ebS3BucketName in aws := None,
    ebS3KeyMapper in aws := identity,
    ebGenerateFilesInBundle in aws := ebGenerateFilesInBundleTask().value,
    ebBuildBundle in aws := ebBuildBundleTask().value,
    ebUploadBundle in aws := ebUploadBundleTask().value,
    // ---
    ebApplicationName in aws := (name in thisProjectRef).value,
    ebApplicationDescription in aws := None,
    ebApplicationCreate in aws := ebCreateApplicationTask().value,
    ebApplicationCreateAndWait in aws := ebCreateApplicationAndWaitTask().value,
    ebApplicationUpdate in aws := ebUpdateApplicationTask().value,
    ebApplicationUpdateAndWait in aws := ebUpdateApplicationAndWaitTask().value,
    ebApplicationDelete in aws := ebDeleteApplicationTask().value,
    ebApplicationDeleteAndWait in aws := ebDeleteApplicationAndWaitTask().value,
    ebApplicationCreateOrUpdate in aws := ebCreateOrUpdateApplicationTask().value,
    ebApplicationCreateOrUpdateAndWait in aws := ebCreateOrUpdateApplicationTask().value,
    // ---
    ebUseBundle in aws := true,
    ebAutoCreateApplication in aws := None,
    ebEnvironmentName in aws := (name in thisProjectRef).value + "-env",
    ebApplicationVersionLabel in aws := (version in thisProjectRef).value + "_" + timestamp,
    ebApplicationVersionDescription in aws := None,
    ebApplicationVersionCreate in aws := ebCreateApplicationVersionTask().value,
    ebApplicationVersionCreateAndWait in aws := ebCreateApplicationVersionAndWaitTask().value,
    ebApplicationVersionUpdate in aws := ebUpdateApplicationVersionTask().value,
    ebApplicationVersionUpdateAndWait in aws := ebUpdateApplicationVersionAndWaitTask().value,
    ebApplicationVersionDelete in aws := ebDeleteApplicationVersionTask().value,
    ebApplicationVersionDeleteAndWait in aws := ebDeleteApplicationVersionAndWaitTask().value,
    ebApplicationVersionCreateOrUpdate in aws := ebCreateOrUpdateApplicationVersionTask().value,
    ebApplicationVersionCreateOrUpdateAndWait in aws := ebCreateOrUpdateApplicationVersionAndWaitTask().value,
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
    ebEnvironmentCreate in aws := ebCreateEnvironmentTask().evaluated,
    ebEnvironmentCreateAndWait in aws := ebCreateEnvironmentAndWaitTask().evaluated,
    ebEnvironmentUpdate in aws := ebUpdateEnvironmentTask().evaluated,
    ebEnvironmentUpdateAndWait in aws := ebUpdateEnvironmentAndWaitTask().evaluated,
    ebEnvironmentCreateOrUpdate in aws := ebCreateOrUpdateEnvironmentTask().evaluated,
    ebEnvironmentCreateOrUpdateAndWait in aws := ebCreateOrUpdateEnvironmentAndWaitTask().evaluated,
    ebRestartAppServer in aws := ebRestartAppServerTask().evaluated,
    // ---
    ebConfigurationTemplate in aws := None,
    ebConfigurationTemplateCreate in aws := ebCreateConfigurationTemplateTask().value,
    ebConfigurationTemplateUpdate in aws := ebUpdateConfigurationTemplateTask().value,
    ebConfigurationTemplateCreateOrUpdate in aws := ebCreateOrUpdateConfigurationTemplateTask().value,
    ebConfigurationTemplateDelete in aws := ebDeleteConfigurationTemplateTask().value,
    // ---
    ebListAvailableSolutionStacks in aws := ebListAvailableSolutionStacksTask().value,
    ebBuildBundle in aws := (ebBuildBundle in aws).dependsOn(ebGenerateFilesInBundle in aws).value
  )

}

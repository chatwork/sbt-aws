package com.chatwork.sbt.aws.eb

import com.chatwork.sbt.aws.core.SbtAwsCoreKeys
import com.chatwork.sbt.aws.eb.SbtAwsEb._
import com.chatwork.sbt.aws.s3.SbtAwsS3Plugin
import sbt.{AutoPlugin, Def, Plugins}

object SbtAwsEbPlugin extends AutoPlugin {

  override def requires: Plugins = SbtAwsS3Plugin

  object autoImport extends SbtAwsEbKeys

  import SbtAwsCoreKeys._
  import SbtAwsEbKeys._

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    ebBundleTargetFiles in aws := Seq.empty,
    ebBundleFileName in aws := "",
    ebS3BucketName in aws := "",
    ebS3KeyCreator in aws := identity,
    ebApplicationName in aws := "",
    ebApplicationDescription in aws := None,
    ebVersionLabel in aws := "1.0.0-SNAPSHOT",
    ebBuildBundle in aws <<= ebBuildBundleTask(),
    ebUploadBundle in aws <<= ebUploadBundleTask(),
    ebCreateApplication in aws <<= ebCreateApplicationTask,
    ebCreateApplicationVersion in aws <<= ebCreateApplicationVersionTask
  )

}

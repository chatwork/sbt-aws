package com.github.j5ik2o.aws

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.model.ObjectMetadata
import com.github.j5ik2o.aws.SbtAws._
import sbt._

object SbtAwsPlugin extends AutoPlugin {

  override def trigger = allRequirements

  object autoImport {

    val AwsKeys = com.github.j5ik2o.aws.AwsKeys

  }

  import AwsKeys.EBKeys._
  import AwsKeys.S3Keys._
  import AwsKeys._

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    credentialProfileName in aws := "default",
    region in aws := Regions.AP_NORTHEAST_1,
    s3OverwriteObject in aws := false,
    s3ObjectMetadata in aws := None,
    s3Upload in aws <<= s3UploadTask,
    ebApplicationName in aws := "",
    ebApplicationDescription in aws := None,
    ebVersionLabel in aws := "1.0.0-SNAPSHOT",
    ebCreateApplication in aws <<= ebCreateApplicationTask,
    ebCreateApplicationVersion in aws <<= ebCreateApplicationVersionTask
  )

}

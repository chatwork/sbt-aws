package org.sisioh.sbt.aws

import com.amazonaws.regions.Regions
import SbtAws._
import org.sisioh.config.{Configuration => SisiohConfiguration}
import org.sisioh.sbt.aws
import sbt.Keys._
import sbt._

object SbtAwsPlugin extends AutoPlugin {

  override def trigger = allRequirements

  object autoImport {

    val AwsKeys = aws.AwsKeys

  }

  import AwsKeys.CfnKeys._
  import AwsKeys.EBKeys._
  import AwsKeys.S3Keys._
  import AwsKeys._

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    credentialProfileName in aws := "default",
    region in aws := Regions.AP_NORTHEAST_1,
    environmentName in aws := System.getProperty("env", "dev"),
    configFile in aws := file((environmentName in aws).value + ".conf"),
    config in aws := SisiohConfiguration.parseFile((configFile in aws).value),
    s3OverwriteObject in aws := false,
    s3ObjectMetadata in aws := None,
    s3Upload in aws <<= s3UploadTask,
    ebApplicationName in aws := "",
    ebApplicationDescription in aws := None,
    ebVersionLabel in aws := "1.0.0-SNAPSHOT",
    ebCreateApplication in aws <<= ebCreateApplicationTask,
    ebCreateApplicationVersion in aws <<= ebCreateApplicationVersionTask,
    cfnTemplatesSourceFolder in aws <<= baseDirectory {
      base => base / "src/main/aws"
    },
    cfnTemplates in aws := {
      val templates = (cfnTemplatesSourceFolder in aws).value ** GlobFilter("*.template")
      templates.get
    },
    watchSources <++= (cfnTemplates in aws) map identity,
    cfnStackName in aws := (config in aws).value.getStringValue("cfn-stack-name").get,
    cfnStackValidate in aws <<= stackValidateTask,
    cfnStackCapabilities in aws := Seq(),
    cfnStackDescribe <<= describeStacksTask().map(s => s.headOption)
  )

}

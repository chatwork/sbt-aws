package org.sisioh.sbt.aws

import com.amazonaws.regions.Regions
import org.sisioh.config.{ Configuration => SisiohConfiguration }
import org.sisioh.sbt.aws.SbtAws._
import sbt.Keys._
import sbt._

object SbtAwsPlugin extends AutoPlugin {

  override def trigger = allRequirements

  object autoImport {

    val AwsKeys = org.sisioh.sbt.aws.AwsKeys

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
      base => base / "src/main/aws"
    },
    cfnTemplates in aws := {
      val templates = (cfnTemplatesSourceFolder in aws).value ** GlobFilter("*.template")
      templates.get
    },
    cfnStackTemplate in aws <<= stackTemplatesTask,
    cfnStackParams in aws := {
      val r = (config in aws).value.getConfiguration(cfnStackParams.key.label)
        .map(_.entrySet.map { case (k, v) => (k, v.render()) }.toMap).getOrElse(Map.empty)
      r
    },
    cfnStackTags in aws := Map.empty,
    cfnStackCapabilities in aws := Seq.empty,
    cfnStackRegion in aws := "",
    cfnStackName in aws := {
      (config in aws).value.getStringValue("cfn-stack-name").get
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
    watchSources <++= (cfnTemplates in aws) map identity
  )

}


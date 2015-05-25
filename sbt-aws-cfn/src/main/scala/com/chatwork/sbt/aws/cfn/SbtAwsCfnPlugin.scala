package com.chatwork.sbt.aws.cfn

import com.chatwork.sbt.aws.cfn.SbtAwsCfn._
import com.chatwork.sbt.aws.core.SbtAwsCoreKeys
import com.chatwork.sbt.aws.s3.SbtAwsS3Plugin
import sbt.Keys._
import sbt._
import org.sisioh.config.{ Configuration => SisiohConfiguration }

import scala.reflect.ClassTag

object SbtAwsCfnPlugin extends AutoPlugin {

  override def trigger = allRequirements

  override def requires: Plugins = SbtAwsS3Plugin

  object autoImport extends SbtAwsCfnKeys

  import SbtAwsCfnKeys._
  import SbtAwsCoreKeys._

  def getConfigValues[A : ClassTag](config: SisiohConfiguration, settingKey: SettingKey[Seq[A]], defaultValue: Seq[A]): Seq[A] = {
    implicitly[ClassTag[Seq[A]]].runtimeClass match {
      case x if x == classOf[Seq[String]] =>
        config.getStringValues(settingKey.key.label).getOrElse(defaultValue).asInstanceOf[Seq[A]]
      case x if x == classOf[Seq[Int]] =>
        config.getIntValues(settingKey.key.label).getOrElse(defaultValue).asInstanceOf[Seq[A]]
      case x if x == classOf[Seq[Boolean]] =>
        config.getBooleanValues(settingKey.key.label).getOrElse(defaultValue).asInstanceOf[Seq[A]]
      case x if x == classOf[Seq[Byte]] =>
        config.getByteValues(settingKey.key.label).getOrElse(defaultValue).asInstanceOf[Seq[A]]
      case x if x == classOf[Seq[Long]] =>
        config.getLongValues(settingKey.key.label).getOrElse(defaultValue).asInstanceOf[Seq[A]]
      case x if x == classOf[Seq[Double]] =>
        config.getDoubleValues(settingKey.key.label).getOrElse(defaultValue).asInstanceOf[Seq[A]]
    }
  }

  def getConfigValueOpt[A : ClassTag](config: SisiohConfiguration, settingKey: SettingKey[_]): Option[A] = {
    implicitly[ClassTag[A]].runtimeClass match {
      case x if x == classOf[String] =>
        config.getStringValue(settingKey.key.label).asInstanceOf[Option[A]]
      case x if x == classOf[Int] =>
        config.getIntValue(settingKey.key.label).asInstanceOf[Option[A]]
      case x if x == classOf[Boolean] =>
        config.getBooleanValue(settingKey.key.label).asInstanceOf[Option[A]]
      case x if x == classOf[Byte] =>
        config.getByteValue(settingKey.key.label).asInstanceOf[Option[A]]
      case x if x == classOf[Long] =>
        config.getLongValue(settingKey.key.label).asInstanceOf[Option[A]]
      case x if x == classOf[Double] =>
        config.getDoubleValue(settingKey.key.label).asInstanceOf[Option[A]]
    }
  }

  def getConfigValue[A : ClassTag](config: SisiohConfiguration, settingKey: SettingKey[_], defaultValue: A) =
    getConfigValueOpt(config, settingKey).getOrElse(defaultValue)

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    cfnTemplatesSourceFolder in aws <<= baseDirectory {
      base => base / defaultTemplateDirectory
    },
    cfnTemplates in aws := {
      val templates = (cfnTemplatesSourceFolder in aws).value ** GlobFilter("*.template")
      templates.get
    },
    cfnArtifactId := {
      getConfigValue[String]((awsConfig in aws).value, cfnArtifactId, (name in ThisProject).value)
    },
    cfnVersion := {
      getConfigValue((awsConfig in aws).value, cfnVersion, (version in ThisProject).value)
    },
    cfnS3BucketName in aws := {
      getConfigValue((awsConfig in aws).value, cfnS3BucketName, "cfn-template")
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
      getConfigValues((awsConfig in aws).value, cfnStackCapabilities, Seq.empty)
    },
    cfnStackRegion in aws := "",
    cfnStackName in aws := {
      getConfigValueOpt[String]((awsConfig in aws).value, cfnStackName)
    },
    cfnCapabilityIam in aws := {
      getConfigValue((awsConfig in aws).value, cfnCapabilityIam, false)
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

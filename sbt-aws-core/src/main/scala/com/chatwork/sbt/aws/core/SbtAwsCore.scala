package com.chatwork.sbt.aws.core

import java.io.File

import com.amazonaws.AmazonWebServiceClient
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{ AWSCredentialsProviderChain, EnvironmentVariableCredentialsProvider, InstanceProfileCredentialsProvider, SystemPropertiesCredentialsProvider }
import com.amazonaws.regions.Region
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.sisioh.config.{ Configuration => SisiohConfiguration }
import sbt.SettingKey

import scala.reflect.ClassTag

import sbt._

object SbtAwsCore extends SbtAwsCore

trait SbtAwsCore {

  protected def newCredentialsProvider(profileName: Option[String]) = {
    new AWSCredentialsProviderChain(
      new EnvironmentVariableCredentialsProvider(),
      new SystemPropertiesCredentialsProvider(),
      new ProfileCredentialsProvider(profileName.orNull),
      new InstanceProfileCredentialsProvider()
    )
  }

  protected def createClient[A <: AmazonWebServiceClient](serviceClass: Class[A], region: Region, profileName: Option[String]): A = {
    region.createClient(serviceClass, newCredentialsProvider(profileName), null)
  }


  protected def md5(file: File): String =
    DigestUtils.md5Hex(IO.readBytes(file))

  def getConfigValuesAsSeq[A: ClassTag](config: SisiohConfiguration, key: String, defaultValue: Seq[A]): Seq[A] = {
    implicitly[ClassTag[Seq[A]]].runtimeClass match {
      case x if x == classOf[Seq[String]] =>
        config.getStringValues(key).getOrElse(defaultValue).asInstanceOf[Seq[A]]
      case x if x == classOf[Seq[Int]] =>
        config.getIntValues(key).getOrElse(defaultValue).asInstanceOf[Seq[A]]
      case x if x == classOf[Seq[Boolean]] =>
        config.getBooleanValues(key).getOrElse(defaultValue).asInstanceOf[Seq[A]]
      case x if x == classOf[Seq[Byte]] =>
        config.getByteValues(key).getOrElse(defaultValue).asInstanceOf[Seq[A]]
      case x if x == classOf[Seq[Long]] =>
        config.getLongValues(key).getOrElse(defaultValue).asInstanceOf[Seq[A]]
      case x if x == classOf[Seq[Double]] =>
        config.getDoubleValues(key).getOrElse(defaultValue).asInstanceOf[Seq[A]]
    }
  }

  def getConfigValueOpt[A: ClassTag](config: SisiohConfiguration, key: String): Option[A] = {
    implicitly[ClassTag[A]].runtimeClass match {
      case x if x == classOf[String] =>
        config.getStringValue(key).asInstanceOf[Option[A]]
      case x if x == classOf[Int] =>
        config.getIntValue(key).asInstanceOf[Option[A]]
      case x if x == classOf[Boolean] =>
        config.getBooleanValue(key).asInstanceOf[Option[A]]
      case x if x == classOf[Byte] =>
        config.getByteValue(key).asInstanceOf[Option[A]]
      case x if x == classOf[Long] =>
        config.getLongValue(key).asInstanceOf[Option[A]]
      case x if x == classOf[Double] =>
        config.getDoubleValue(key).asInstanceOf[Option[A]]
    }
  }

  def getConfigValue[A: ClassTag](config: SisiohConfiguration, key: String, defaultValue: A) =
    getConfigValueOpt(config, key).getOrElse(defaultValue)

  def getConfigValuesAsMap(config: SisiohConfiguration, key: String): Map[String, String] = {
    config.getConfiguration(key)
      .map(_.entrySet.map { case (k, v) => (k, v.unwrapped().toString) }.toMap).getOrElse(Map.empty)
  }

}
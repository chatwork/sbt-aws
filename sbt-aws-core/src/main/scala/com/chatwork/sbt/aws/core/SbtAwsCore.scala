package com.chatwork.sbt.aws.core

import java.io.File

import com.amazonaws.AmazonWebServiceClient
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{ InstanceProfileCredentialsProvider, SystemPropertiesCredentialsProvider, EnvironmentVariableCredentialsProvider, AWSCredentialsProviderChain }
import com.amazonaws.regions.Region
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils

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
    DigestUtils.md5Hex(FileUtils.readFileToByteArray(file))

}
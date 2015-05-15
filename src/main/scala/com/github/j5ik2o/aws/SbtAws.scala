package com.github.j5ik2o.aws

import java.io.File

import com.amazonaws.AmazonWebServiceClient
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{AWSCredentialsProviderChain, EnvironmentVariableCredentialsProvider, InstanceProfileCredentialsProvider, SystemPropertiesCredentialsProvider}
import com.amazonaws.regions.Region
import com.amazonaws.services.elasticbeanstalk._
import com.amazonaws.services.elasticbeanstalk.model._
import com.amazonaws.services.s3._
import com.amazonaws.services.s3.model._
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.sisioh.aws4s.eb.Implicits._
import org.sisioh.aws4s.eb.model._
import org.sisioh.aws4s.s3.Implicits._
import sbt._
import Keys._
import AwsKeys._
import AwsKeys.S3Keys._
import AwsKeys.EBKeys._

import scala.util.{Failure, Success, Try}

object SbtAws extends SbtAwsS3 with SbtAwsEB {

  private[aws] def newCredentialsProvider(profileName: String) = {
    new AWSCredentialsProviderChain(
      new EnvironmentVariableCredentialsProvider(),
      new SystemPropertiesCredentialsProvider(),
      new ProfileCredentialsProvider(profileName),
      new InstanceProfileCredentialsProvider()
    )
  }

  private[aws] def createClient[A <: AmazonWebServiceClient](serviceClass: Class[A], region: Region, profileName: String): A = {
    region.createClient(serviceClass, newCredentialsProvider(profileName), null)
  }

  private[aws] def md5(file: File): String =
    DigestUtils.md5Hex(FileUtils.readFileToByteArray(file))


}





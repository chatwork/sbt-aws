package org.sisioh.sbt.aws

import java.io.File

import com.amazonaws.AmazonWebServiceClient
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{AWSCredentialsProviderChain, EnvironmentVariableCredentialsProvider, InstanceProfileCredentialsProvider, SystemPropertiesCredentialsProvider}
import com.amazonaws.regions.Region
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils

object SbtAws extends SbtAwsS3 with SbtAwsEB with SbtAwsCfn {

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





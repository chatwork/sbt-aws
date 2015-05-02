package com.github.j5ik2o.aws

import com.amazonaws.AmazonWebServiceClient
import com.amazonaws.auth._
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{AmazonS3Exception, ObjectMetadata, PutObjectRequest}
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.sisioh.aws4s.s3.Implicits._
import sbt.Keys._
import sbt._
import sbt.plugins.JvmPlugin

object SbtAwsPlugin extends AutoPlugin {

  override def requires: Plugins = JvmPlugin

  override def trigger = allRequirements

  object autoImport {

    val regionName = SettingKey[String]("aws-region-name")
    val credentialProfileName = SettingKey[String]("aws-credential-profile-name")

    val bucketName = SettingKey[String]("s3-bucket-name")
    val key = SettingKey[String]("s3-key")
    val overwrite = SettingKey[Boolean]("s3-overwrite-object")
    val file = SettingKey[File]("s3-file")
    val resourceUrl = SettingKey[URL]("s3-resource-url")
    val objectMetadata = SettingKey[ObjectMetadata]("s3-object-metadata")
    val upload = TaskKey[Unit]("s3-upload", "Uploads files to an S3 bucket.")

  }

  private def newCredentialsProvider(profileName: String) = {
    new AWSCredentialsProviderChain(
      new EnvironmentVariableCredentialsProvider(),
      new SystemPropertiesCredentialsProvider(),
      new ProfileCredentialsProvider(profileName),
      new InstanceProfileCredentialsProvider()
    )
  }

  private def createClient[A <: AmazonWebServiceClient](serviceClass: Class[A], region: Region, profileName: String): A = {
    region.createClient(serviceClass, newCredentialsProvider(profileName), null)
  }

  private def exists(client: AmazonS3Client, bucketName: String, key: String): Boolean = {
    client.requestGetObjectMetadata(bucketName, key).map(_ => true).recover {
      case ex: AmazonS3Exception if ex.getStatusCode == 404 => true
      case ex: AmazonS3Exception => false
    }.get
  }

  private def existingObjectMetadata(client: AmazonS3Client, bucketName: String, key: String): Option[ObjectMetadata] =
    client.requestGetObjectMetadata(bucketName, key).toOption

  private def md5(file: File): String =
    DigestUtils.md5Hex(FileUtils.readFileToByteArray(file))

  import autoImport._

  def s3Upload(logger: Logger,
               regionName: String, credentialProfileName: String,
               bucketName: String, key: String, overwrite: Boolean, file: File, objectMetadata: ObjectMetadata) = {
    val client = createClient(classOf[AmazonS3Client], Region.getRegion(Regions.fromName(regionName)), credentialProfileName)
    val metadata = existingObjectMetadata(client, bucketName, key)
    logger.debug("check-1")
    if (metadata.isEmpty || (overwrite && metadata.get.getETag != md5(file))) {
      logger.debug("check-2")
      client.requestPutObject(new PutObjectRequest(bucketName, key, file).withMetadata(objectMetadata)).get
      logger.debug("check-3")
    }
    logger.debug("check-4")
  }

  val aws4sVersion = "1.0.2-SNAPSHOT"

  val awsSdkVersion = "1.9.22"

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    credentialProfileName := "default",
    regionName := Regions.AP_NORTHEAST_1.getName,
    upload <<= (streams, regionName, credentialProfileName,
      bucketName, key, overwrite, file, objectMetadata) map { (stream, rn, cpn, bn, k, ow, f, om) =>
      s3Upload(stream.log, rn, cpn, bn, k, ow, f, om)
    }
  )

}

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

import scala.util.{Success, Try}

object SbtAwsPlugin extends AutoPlugin {

  override def trigger = allRequirements

  object autoImport {

    object AWS {

      val regionName = SettingKey[String]("aws-region-name")
      val credentialProfileName = SettingKey[String]("aws-credential-profile-name")

      object S3 {

        val bucketName = SettingKey[String]("s3-bucket-name")
        val key = SettingKey[String]("s3-key")
        val overwriteObject = SettingKey[Boolean]("s3-overwrite-object")
        val file = SettingKey[File]("s3-file")
        val resourceUrl = SettingKey[String]("s3-resource-url")
        val objectMetadata = SettingKey[ObjectMetadata]("s3-object-metadata")

        val upload = TaskKey[Unit]("s3-upload", "Uploads files to an S3 bucket.")

      }

    }

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
  import AWS.S3._
  import AWS._

  private def s3Upload(logger: Logger,
                       regionName: String, credentialProfileName: String,
                       bucketName: String, key: String,
                       overwrite: Boolean,
                       file: File, objectMetadata: ObjectMetadata): Try[Unit] = {
    val client = createClient(classOf[AmazonS3Client], Region.getRegion(Regions.fromName(regionName)), credentialProfileName)
    val metadata = existingObjectMetadata(client, bucketName, key)
    if (metadata.isEmpty || (overwrite && metadata.get.getETag != md5(file))) {
      client.requestPutObject(new PutObjectRequest(bucketName, key, file).withMetadata(objectMetadata)).flatMap{ result =>
        client.requestGetResourceUrl(bucketName, key).map(_ => ())
      }
    } else {
      Success(())
    }
  }


  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    credentialProfileName := "default",
    regionName := Regions.AP_NORTHEAST_1.getName,
    overwriteObject := false,
    objectMetadata := new ObjectMetadata(),
    upload <<= (streams, regionName, credentialProfileName,
      bucketName, key, overwriteObject, file, objectMetadata) map { (stream, r, cpn, bn, k, ow, f, om) =>
      s3Upload(stream.log, r, cpn, bn, k, ow, f, om)
    }
  )

}

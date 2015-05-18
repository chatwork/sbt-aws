package org.sisioh.sbt.aws

import com.amazonaws.regions.Region
import com.amazonaws.services.s3._
import com.amazonaws.services.s3.model._
import org.sisioh.aws4s.s3.Implicits._
import org.sisioh.aws4s.s3.model.PutObjectRequestFactory
import org.sisioh.sbt.aws.AwsKeys.S3Keys._
import org.sisioh.sbt.aws.AwsKeys._
import sbt._

import scala.util.{ Failure, Success, Try }

trait SbtAwsS3 {
  this: SbtAws.type =>

  lazy val s3Client = Def.task {
    createClient(classOf[AmazonS3Client], Region.getRegion((region in aws).value), (credentialProfileName in aws).value)
  }

  def s3ExistsS3Object(client: AmazonS3Client, bucketName: String, key: String): Try[Boolean] = {
    s3GetS3ObjectMetadata(client, bucketName, key).map(_.isDefined)
  }

  def s3GetS3ObjectMetadata(client: AmazonS3Client, bucketName: String, key: String): Try[Option[ObjectMetadata]] = {
    client.getObjectMetadataAsTry(bucketName, key).map(Some(_)).recoverWith {
      case ex: AmazonS3Exception if ex.getStatusCode() == 404 =>
        Success(None)
      case ex =>
        Failure(ex)
    }
  }

  def isCond(file: File, metadataOpt: Option[ObjectMetadata], overwrite: Boolean) =
    metadataOpt.isEmpty || (overwrite && metadataOpt.get.getETag != md5(file))

  def s3PutObjectAndGetUrl(client: AmazonS3Client,
                           bucketName: String,
                           key: String,
                           file: File,
                           metadataOpt: Option[ObjectMetadata],
                           overwrite: Boolean, createBucket: Boolean) =
    if (isCond(file, metadataOpt, overwrite)) {
      for {
        exist <- client.doesBucketExistAsTry(bucketName)
        _ <- if (!exist && createBucket) {
          client.createBucketAsTry(bucketName)
        } else Success(())
        result <- client.putObjectAsTry(PutObjectRequestFactory.create(bucketName, key, file).withMetadataOpt(metadataOpt))
        resourceUrl <- client.getResourceUrlAsTry(bucketName, key)
      } yield Some(resourceUrl)
    } else Success(None)

  def s3PutObject(client: AmazonS3Client, bucketName: String, key: String, file: File, overwrite: Boolean, createBucket: Boolean) = {
    for {
      metadataOpt <- s3GetS3ObjectMetadata(client, bucketName, key)
      url <- s3PutObjectAndGetUrl(client, bucketName, key, file, metadataOpt, overwrite, createBucket)
    } yield url
  }

  def s3UploadTask: Def.Initialize[Task[Option[String]]] = Def.task {
    val client = s3Client.value
    val bucketName = (s3BucketName in aws).value
    val key = (s3Key in aws).value
    val file = (s3File in aws).value.get
    val overwrite = (s3OverwriteObject in aws).value
    val createBucket = (s3CreateBucket in aws).value
    s3PutObject(client, bucketName, key, file, overwrite, createBucket).get
  }

}
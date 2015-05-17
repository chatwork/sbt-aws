package org.sisioh.sbt.aws

import com.amazonaws.regions.Region
import com.amazonaws.services.s3._
import com.amazonaws.services.s3.model._
import AwsKeys.S3Keys._
import AwsKeys._
import org.sisioh.aws4s.s3.Implicits._
import sbt.Keys._
import sbt._

import scala.util.{Failure, Success, Try}

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

  def s3Upload(logger: Logger,
               client: AmazonS3Client,
               file: File,
               bucketName: String,
               key: String,
               overwrite: Boolean,
               objectMetadataOpt: Option[ObjectMetadata]): Try[Option[String]] = {
    s3GetS3ObjectMetadata(client, bucketName, key).flatMap { metadataOpt =>
      val hash = md5(file)
      val metadataHash = metadataOpt.get.getETag
      logger.debug("file md5 = " + hash)
      logger.debug("metadata etag = " + metadataHash)
      if (metadataOpt.isEmpty || (overwrite && metadataHash != hash)) {
        (for {
          result <- client.putObjectAsTry(
            objectMetadataOpt.fold(new PutObjectRequest(bucketName, key, file)) { om =>
              new PutObjectRequest(bucketName, key, file).withMetadata(om)
            }
          )
          resourceUrl <- client.getResourceUrlAsTry(bucketName, key)
        } yield Some(resourceUrl)).map {
          url =>
            logger.info("uploaded file: url = " + url)
            url
        }
      } else {
        Success(None)
      }
    }
  }

  def s3UploadTask: Def.Initialize[Task[Option[String]]] = Def.task {
    s3Upload(
      streams.value.log,
      s3Client.value,
      (s3File in aws).value,
      (s3BucketName in aws).value,
      (s3Key in aws).value,
      (s3OverwriteObject in aws).value,
      (s3ObjectMetadata in aws).value
    ).get
  }

}
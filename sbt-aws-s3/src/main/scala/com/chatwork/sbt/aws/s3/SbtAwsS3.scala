package com.chatwork.sbt.aws.s3

import com.amazonaws.ClientConfiguration
import com.amazonaws.regions.Region
import com.amazonaws.services.s3.{AmazonS3, AmazonS3Client, AmazonS3ClientBuilder}
import com.amazonaws.services.s3.model.{AmazonS3Exception, ObjectMetadata}
import com.chatwork.sbt.aws.core.SbtAwsCore
import com.chatwork.sbt.aws.core.SbtAwsCoreKeys._
import com.chatwork.sbt.aws.s3.SbtAwsS3Keys._
import org.sisioh.aws4s.s3.Implicits._
import org.sisioh.aws4s.s3.model.PutObjectRequestFactory
import sbt.Keys._
import sbt._

import scala.util.{Failure, Success, Try}

object SbtAwsS3 extends SbtAwsS3

trait SbtAwsS3 extends SbtAwsCore {

  private def getProxyConfiguration: ClientConfiguration = {
    val configuration = new ClientConfiguration()
    for {
      proxyHost <- Option(System.getProperty("https.proxyHost"))
      proxyPort <- Option(System.getProperty("https.proxyPort").toInt)
    } {
      configuration.setProxyHost(proxyHost)
      configuration.setProxyPort(proxyPort)
    }
    configuration
  }

  lazy val s3Client: Def.Initialize[Task[AmazonS3]] = Def.task {
    val logger = streams.value.log
    val r      = (region in aws).value
    val cpc    = (credentialsProviderChain in aws).value
    val cc     = (clientConfiguration in aws).value
    logger.info(s"region = $r")
    val builder0           = AmazonS3ClientBuilder.standard().withRegion(r).withCredentials(cpc)
    val builder            = cc.fold(builder0)(builder0.withClientConfiguration)
    val s3Client: AmazonS3 = builder.build()
    s3Client
  }

  def s3ExistsS3Object(client: AmazonS3, bucketName: String, key: String): Try[Boolean] = {
    s3GetS3ObjectMetadata(client, bucketName, key).map(_.isDefined)
  }

  def s3GetS3ObjectMetadata(client: AmazonS3,
                            bucketName: String,
                            key: String): Try[Option[ObjectMetadata]] = {
    Try(client.getObjectMetadata(bucketName, key)).map(Some(_)).recoverWith {
      case ex: AmazonS3Exception if ex.getStatusCode == 404 =>
        Success(None)
      case ex =>
        Failure(ex)
    }
  }

  def isCond(file: File, metadataOpt: Option[ObjectMetadata], overwrite: Boolean): Boolean =
    metadataOpt.isEmpty || (overwrite && metadataOpt.get.getETag != md5(file))

  def s3PutObjectAndGetUrl(logger: Logger, client: AmazonS3,
                           bucketName: String,
                           key: String,
                           file: File,
                           metadataOpt: Option[ObjectMetadata],
                           overwrite: Boolean,
                           createBucket: Boolean): Try[String] =
    if (isCond(file, metadataOpt, overwrite)) {
      for {
        exist <- Try(client.doesBucketExistV2(bucketName))
        _ <- if (!exist && createBucket) {
          Try(client.createBucket(bucketName))
        } else Success(())
        _ <- Try(client.putObject(
          PutObjectRequestFactory.create(bucketName, key, file).withMetadataOpt(metadataOpt)))
        resourceUrl <- Try(client.getUrl(bucketName, key)).map(_.toString)
      } yield resourceUrl
    } else {
      Try(client.getUrl(bucketName, key)).map(_.toString)
    }

  def s3PutObject(logger: Logger, client: AmazonS3,
                  bucketName: String,
                  key: String,
                  file: File,
                  overwrite: Boolean,
                  createBucket: Boolean): Try[String] = {
    for {
      metadataOpt <- s3GetS3ObjectMetadata(client, bucketName, key)
      url <- s3PutObjectAndGetUrl(logger, client,
                                  bucketName,
                                  key,
                                  file,
                                  metadataOpt,
                                  overwrite,
                                  createBucket)
    } yield url
  }

  def s3UploadTask: Def.Initialize[Task[String]] = Def.task {
    val logger: Logger = streams.value.log
    val client       = s3Client.value
    val bucketName   = (s3BucketName in aws).value
    val key          = (s3Key in aws).value
    val file         = (s3File in aws).value.get
    val overwrite    = (s3OverwriteObject in aws).value
    val createBucket = (s3CreateBucket in aws).value

    logger.info(
      s"put object request : bucketName = $bucketName, key = $key, file = $file, overwrite = $overwrite, createBucket = $createBucket")
    val url = s3PutObject(logger, client, bucketName, key, file, overwrite, createBucket).get
    logger.info(s"put object requested : bucketName = $bucketName key = $key, url = $url")
    url
  }

}

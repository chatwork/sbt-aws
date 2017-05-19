package com.chatwork.sbt.aws.s3.resolver.ivy

import java.io.{File, FileOutputStream, IOException}
import java.util

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model._
import com.chatwork.sbt.aws.s3.resolver.S3Utility
import org.apache.ivy.core.module.descriptor.Artifact
import org.apache.ivy.plugins.repository._
import org.apache.ivy.util.{FileUtil, Message}

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.{Failure, Success, Try}

case class S3Repository(s3Client: AmazonS3Client,
                        region: Region,
                        acl: CannedAccessControlList,
                        serverSideEncryption: Boolean,
                        overwrite: Boolean)
    extends AbstractRepository {

  private val cacheOfS3Resource = mutable.Map.empty[String, S3Resource]

  override def get(source: String, destination: File): Unit = {
    val resource = getResource(source)
    try {
      fireTransferInitiated(resource, TransferEvent.REQUEST_GET)
      val progressListener = new RepositoryCopyProgressListener(this)
      progressListener.setTotalLength(resource.getContentLength())
      FileUtil.copy(resource.openStream(), new FileOutputStream(destination), progressListener)
    } catch {
      case e: IOException =>
        fireTransferError(e)
        throw new Error(e)
      case e: RuntimeException =>
        fireTransferError(e)
        throw e
    } finally fireTransferCompleted(resource.getContentLength)
  }

  override def put(artifact: Artifact,
                   source: File,
                   destination: String,
                   overwrite: Boolean): Unit = {
    val bucket  = S3Utility.getBucket(destination)
    val key     = S3Utility.getKey(destination)
    val request = new PutObjectRequest(bucket, key, source).withCannedAcl(acl)

    if (serverSideEncryption) {
      val objectMetadata = new ObjectMetadata()
      objectMetadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION)
      request.setMetadata(objectMetadata)
    }

    if (!s3Client.doesBucketExist(bucket)) {
      if (!createBucket(bucket, region)) {
        throw new Error("couldn't create bucket")
      }
    }

    if (!this.overwrite && !s3Client.listObjects(bucket, key).getObjectSummaries.isEmpty) {
      throw new Error(destination + " exists but overwriting is disabled")
    }

    s3Client.putObject(request)
  }

  override def list(parent: String): util.List[_] = {
    try {
      @tailrec
      def getKeys(cond: Boolean, marker: Option[String], list: List[String]): List[String] = {
        if (marker.isEmpty) list
        else {
          val request = new ListObjectsRequest()
            .withBucketName(S3Utility.getBucket(parent))
            .withPrefix(S3Utility.getKey(parent))
            .withDelimiter("/")
            .withMarker(marker.orNull)
          val listing = s3Client.listObjects(request)
          val next = listing.getCommonPrefixes.asScala ++ listing.getObjectSummaries.asScala
            .map(_.getKey)
          val markerOpt = Option(listing.getMarker)
          getKeys(markerOpt.isDefined, marker, next.toList)
        }
      }
      getKeys(cond = true, None, List.empty).asJava
    } catch {
      case e: AmazonServiceException =>
        throw S3RepositoryException(e)
    }
  }

  override def getResource(source: String): Resource = {
    if (!cacheOfS3Resource.contains(source)) {
      cacheOfS3Resource.put(source, S3Resource(s3Client, source))
    }
    cacheOfS3Resource(source)
  }

  private def createBucket(name: String, region: Region): Boolean = {
    val timeout = 1000 * 20
    @tailrec
    def create0(retryCount: Int, result: Boolean): Boolean = {
      if (retryCount == 0)
        result
      else {
        Try(s3Client.createBucket(name, region)) match {
          case Success(_) =>
            if (s3Client.doesBucketExist(name))
              true
            else
              false
          case Failure(ex: AmazonS3Exception) =>
            Message.warn(ex.getMessage)
            Thread.sleep(timeout)
            create0(retryCount - 1, result = false)
          case Failure(ex: Throwable) =>
            throw ex
        }
      }
    }
    create0(5, result = false)
  }

}

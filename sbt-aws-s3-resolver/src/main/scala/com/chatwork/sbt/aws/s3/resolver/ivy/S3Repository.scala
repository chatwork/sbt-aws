package com.chatwork.sbt.aws.s3.resolver.ivy

import java.io.{ File, FileOutputStream, IOException }
import java.util

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model._
import com.chatwork.sbt.aws.s3.resolver.S3Utils
import org.apache.ivy.core.module.descriptor.Artifact
import org.apache.ivy.plugins.repository._
import org.apache.ivy.util.{ FileUtil, Message }

import scala.collection.JavaConverters._
import scala.collection.mutable

case class S3Repository(s3Client: AmazonS3Client,
                        region: Region,
                        acl: CannedAccessControlList = CannedAccessControlList.PublicRead,
                        serverSideEncryption: Boolean = false,
                        overwrite: Boolean = false) extends AbstractRepository {

  private val resourceCache = mutable.Map.empty[String, S3Resource]

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

  override def put(artifact: Artifact, source: File, destination: String, overwrite: Boolean): Unit = {
    val bucket = S3Utils.getBucket(destination)
    val key = S3Utils.getKey(destination)
    var request = new PutObjectRequest(bucket, key, source)
    request = request.withCannedAcl(acl)

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
      var marker: String = null
      val keys = new util.ArrayList[String]()

      do {
        val request = new ListObjectsRequest()
          .withBucketName(S3Utils.getBucket(parent))
          .withPrefix(S3Utils.getKey(parent))
          .withDelimiter("/") // RFC 2396
          .withMarker(marker)

        val listing = s3Client.listObjects(request)

        // Add "directories"
        keys.addAll(listing.getCommonPrefixes)

        // Add "files"
        for (summary <- listing.getObjectSummaries.asScala) {
          keys.add(summary.getKey)
        }

        marker = listing.getNextMarker
      } while (marker != null)

      keys
    } catch {
      case e: AmazonServiceException =>
        throw S3RepositoryException(e)
    }
  }

  override def getResource(source: String): Resource = {
    if (!resourceCache.contains(source)) {
      resourceCache.put(source, S3Resource(s3Client, source))
    }
    resourceCache(source)
  }

  private def createBucket(name: String, region: Region): Boolean = {
    val attemptLimit = 5
    val timeout = 1000 * 20
    var attempt = 0

    while (attempt < attemptLimit) {
      try {
        attempt += 1

        s3Client.createBucket(name, region)
        if (s3Client.doesBucketExist(name)) {
          return true
        }

      } catch {
        case s3e: AmazonS3Exception =>
          try {
            Message.warn(s3e.toString)
            Thread.sleep(timeout);
          } catch {
            case e: InterruptedException =>
          }
      }
    }
    false
  }

}

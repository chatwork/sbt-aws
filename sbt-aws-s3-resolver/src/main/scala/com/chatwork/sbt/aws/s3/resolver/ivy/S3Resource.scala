package com.chatwork.sbt.aws.s3.resolver.ivy

import java.io.InputStream

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.s3.AmazonS3Client
import com.chatwork.sbt.aws.s3.resolver.S3Utils
import org.apache.ivy.plugins.repository.Resource

case class S3Resource(s3Client: AmazonS3Client, uri: String) extends Resource {

  val bucket = S3Utils.getBucket(uri)

  val key = S3Utils.getKey(uri)

  private var _exists: Boolean = false
  private var contentLength: Long = 0L
  private var lastModified: Long = 0L

  try {
    val metadata = s3Client.getObjectMetadata(bucket, key)
    _exists = true
    contentLength = metadata.getContentLength
    lastModified = metadata.getLastModified.getTime
  } catch {
    case ex: AmazonServiceException =>
  }

  override def getName: String = uri

  override def isLocal: Boolean = false

  override def openStream(): InputStream = {
    try {
      s3Client.getObject(bucket, key).getObjectContent
    } catch {
      case ex: AmazonServiceException =>
        throw S3RepositoryException(ex)
    }
  }

  override def clone(cloneName: String): Resource = S3Resource(
    s3Client, cloneName
  )

  override def getLastModified: Long = lastModified

  override def getContentLength: Long = contentLength

  override def exists(): Boolean = _exists

}

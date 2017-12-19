package com.chatwork.sbt.aws.s3.resolver.ivy

import java.io.InputStream

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.s3.model.S3Object
import org.apache.ivy.plugins.repository.Resource

case class S3Resource(s3Object: S3Object, name: String) extends Resource {

  override def getName: String = name

  override def isLocal: Boolean = false

  override def openStream(): InputStream = {
    try {
      s3Object.getObjectContent
    } catch {
      case ex: AmazonServiceException =>
        throw S3RepositoryException(ex)
    }
  }

  override def clone(name: String): Resource = copy(name = name)

  override def getLastModified: Long = s3Object.getObjectMetadata.getLastModified.getTime

  override def getContentLength: Long = s3Object.getObjectMetadata.getContentLength

  override def exists(): Boolean = true

}

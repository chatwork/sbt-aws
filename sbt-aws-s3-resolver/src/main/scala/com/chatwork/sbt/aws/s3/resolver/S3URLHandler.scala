package com.chatwork.sbt.aws.s3.resolver

import java.io.{File, InputStream}
import java.net.URL

import com.amazonaws.ClientConfiguration
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model._
import com.chatwork.sbt.aws.s3.SbtAwsS3Keys
import com.chatwork.sbt.aws.s3.resolver.S3URLHandler.S3URLInfo
import org.apache.ivy.util.url.{URLHandler, URLHandlerDispatcher, URLHandlerRegistry}
import org.apache.ivy.util.{CopyProgressEvent, CopyProgressListener}

object S3URLHandler {

  def setup(s3Client: AmazonS3Client,
            sse: Boolean,
            acl: SbtAwsS3Keys.S3ACL,
            overwrite: Boolean): Unit = {
    val dispatcher: URLHandlerDispatcher = URLHandlerRegistry.getDefault match {
      // If the default is already a URLHandlerDispatcher then just use that
      case disp: URLHandlerDispatcher =>
        disp
      // Otherwise create a new URLHandlerDispatcher
      case default =>
        val disp: URLHandlerDispatcher = new URLHandlerDispatcher()
        disp.setDefault(default)
        URLHandlerRegistry.setDefault(disp)
        disp
    }

    // Register (or replace) the s3 handler
    dispatcher.setDownloader("s3", new S3URLHandler(s3Client, sse, Some(acl), Some(overwrite)))
  }

  private class S3URLInfo(available: Boolean, contentLength: Long, lastModified: Long)
      extends URLHandler.URLInfo(available, contentLength, lastModified)

}

case class S3URLHandler(s3Client: AmazonS3Client,
                        sse: Boolean,
                        acl: Option[SbtAwsS3Keys.S3ACL] = None,
                        overwrite: Option[Boolean] = None)
    extends URLHandler {

  def getProxyConfiguration: ClientConfiguration = {
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

  override def setRequestMethod(requestMethod: Int): Unit = ()

  override def isReachable(url: URL): Boolean = getURLInfo(url).isReachable

  override def isReachable(url: URL, timeout: Int): Boolean = getURLInfo(url, timeout).isReachable

  override def getLastModified(url: URL): Long = getURLInfo(url).getLastModified

  override def getLastModified(url: URL, timeout: Int): Long =
    getURLInfo(url, timeout).getLastModified

  override def upload(src: File, dest: URL, l: CopyProgressListener): Unit = {
    val bucket = S3Utility.getBucket(dest.toString)
    val key    = S3Utility.getKey(dest.toString)

    def putImpl(serverSideEncryption: Boolean): PutObjectResult = {
      val meta: ObjectMetadata = new ObjectMetadata()
      if (serverSideEncryption) meta.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION)
      if (!overwrite.getOrElse(true) && !s3Client
            .listObjects(bucket, key)
            .getObjectSummaries
            .isEmpty) {
        throw new Error(dest + " exists but overwriting is disabled")
      }

      val defaultRequest = new PutObjectRequest(bucket, key, src).withMetadata(meta)
      val request        = acl.fold(defaultRequest)(v => defaultRequest.withCannedAcl(v))
      s3Client.putObject(request)
    }

    val eventOpt = Option(new CopyProgressEvent())
    eventOpt.foreach(event => l.start(event))
    try {
      putImpl(sse)
    } catch {
      case ex: AmazonS3Exception if ex.getStatusCode == 403 && sse =>
        putImpl(true)
    }
    eventOpt.foreach(event => l.end(event))
  }

  override def getContentLength(url: URL): Long = getURLInfo(url).getContentLength

  override def getURLInfo(url: URL): URLHandler.URLInfo = getURLInfo(url, 0)

  override def getURLInfo(url: URL, timeout: Int): URLHandler.URLInfo =
    try {
      val bucket               = S3Utility.getBucket(url.toString)
      val key                  = S3Utility.getKey(url.toString)
      val meta: ObjectMetadata = s3Client.getObjectMetadata(bucket, key)
      val available: Boolean   = true
      val contentLength: Long  = meta.getContentLength
      val lastModified: Long   = meta.getLastModified.getTime
      new S3URLInfo(available, contentLength, lastModified)
    } catch {
      case ex: AmazonS3Exception if ex.getStatusCode == 404 => URLHandler.UNAVAILABLE
    }

  override def getContentLength(url: URL, timeout: Int): Long =
    getURLInfo(url, timeout).getContentLength

  override def openStream(url: URL): InputStream = {
    val bucket = S3Utility.getBucket(url.toString)
    val key    = S3Utility.getKey(url.toString)
    val obj    = s3Client.getObject(bucket, key)
    obj.getObjectContent
  }

  override def download(src: URL, dest: File, l: CopyProgressListener): Unit = {
    val bucket   = S3Utility.getBucket(src.toString)
    val key      = S3Utility.getKey(src.toString)
    val eventOpt = Option(new CopyProgressEvent())
    eventOpt.foreach(event => l.start(event))

    val meta: ObjectMetadata = s3Client.getObject(new GetObjectRequest(bucket, key), dest)
    dest.setLastModified(meta.getLastModified.getTime)
    eventOpt.foreach(event => l.end(event))
  }
}

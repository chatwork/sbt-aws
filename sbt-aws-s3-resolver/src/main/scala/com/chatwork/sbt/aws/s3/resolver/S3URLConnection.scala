package com.chatwork.sbt.aws.s3.resolver

import java.io.InputStream
import java.net.{HttpURLConnection, URL}

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{ObjectMetadata, S3Object}

final class S3URLConnection(val s3Client: AmazonS3Client, sse: Boolean, url: URL)
    extends HttpURLConnection(url) {
  val bucket                             = S3Utility.getBucket(url.toString)
  val key                                = S3Utility.getKey(url.toString)
  private val s3URLHandler: S3URLHandler = new S3URLHandler(s3Client, sse)

  private trait S3Response extends AutoCloseable {
    def meta: ObjectMetadata
    def inputStream: Option[InputStream]
  }

  private case class HEADResponse(meta: ObjectMetadata) extends S3Response {
    def close(): Unit                    = {}
    def inputStream: Option[InputStream] = None
  }

  private case class GETResponse(obj: S3Object) extends S3Response {
    def meta: ObjectMetadata             = obj.getObjectMetadata
    def inputStream: Option[InputStream] = Option(obj.getObjectContent)
    def close(): Unit                    = obj.close()
  }

  private[this] var response: Option[S3Response] = None

  override def disconnect(): Unit = response.foreach { _.close() }

  override def usingProxy(): Boolean =
    Option(s3URLHandler.getProxyConfiguration.getProxyHost).exists {
      _ != ""
    }

  override def getInputStream: InputStream = {
    if (!connected) connect()
    response.flatMap { _.inputStream }.orNull
  }

  override def getHeaderField(field: String): String = {
    if (!connected) connect()

    field.toLowerCase match {
      case "content-type"     => response.map { _.meta.getContentType }.orNull
      case "content-encoding" => response.map { _.meta.getContentEncoding }.orNull
      case "content-length"   => response.map { _.meta.getContentLength.toString }.orNull
      case "last-modified"    => response.map { _.meta.getLastModified.getTime.toString }.orNull
      case _                  => ""
    }
  }

  override def connect(): Unit = {
    response = getRequestMethod.toLowerCase match {
      case "head" =>
        Option(HEADResponse(s3Client.getObjectMetadata(bucket, key)))
      case "get" => Option(GETResponse(s3Client.getObject(bucket, key)))
      case _     => throw new IllegalArgumentException("Invalid request method: " + getRequestMethod)
    }
    responseCode = if (response.isEmpty) 404 else 200
    connected = true
  }
}

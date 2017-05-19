package com.chatwork.sbt.aws.s3.resolver

import java.net.{URL, URLConnection, URLStreamHandler, URLStreamHandlerFactory}

import com.amazonaws.services.s3.AmazonS3Client

class S3StreamHandler(s3Client: AmazonS3Client, sse: Boolean) extends URLStreamHandler {

  override def openConnection(url: URL): URLConnection = new S3URLConnection(s3Client, sse, url)

}

object S3StreamHandler {

  def setup(s3Client: AmazonS3Client, sse: Boolean): Unit =
    URL.setURLStreamHandlerFactory(new S3URLStreamHandlerFactory(s3Client, sse: Boolean))

  private class S3URLStreamHandlerFactory(s3Client: AmazonS3Client, sse: Boolean)
      extends URLStreamHandlerFactory {
    def createURLStreamHandler(protocol: String): URLStreamHandler = protocol match {
      case "s3" => new S3StreamHandler(s3Client, sse)
      case _    => null
    }
  }

}

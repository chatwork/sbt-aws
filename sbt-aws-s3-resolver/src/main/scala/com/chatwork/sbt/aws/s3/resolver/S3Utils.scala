package com.chatwork.sbt.aws.s3.resolver

import java.net.{ URI, URISyntaxException }

object S3Utils {

  def getBucket(uri: String): String = {
    getUri(uri).getHost
  }

  def getKey(uri: String): String = {
    getUri(uri).getPath.substring(1)
  }

  private def getUri(uri: String): URI = {
    try {
      new URI(uri)
    } catch {
      case ex: URISyntaxException =>
        throw new IllegalArgumentException("'" + uri + "' is a malformed S3 URI")
    }
  }

}

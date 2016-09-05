package com.chatwork.sbt.aws.s3.resolver.ivy

import java.io.{ File, InputStream }
import java.net.{ InetAddress, URI, URL }

import com.amazonaws.ClientConfiguration
import com.amazonaws.regions.{ RegionUtils, Regions }
import com.amazonaws.services.s3.model._
import com.amazonaws.services.s3.{ AmazonS3Client, AmazonS3URI }
import org.apache.ivy.util.{ CopyProgressEvent, CopyProgressListener, Message }
import org.apache.ivy.util.url.URLHandler
import org.apache.ivy.util.url.URLHandler.URLInfo

import scala.util.Try
import scala.util.matching.Regex

object S3URLHandler {
  private val RegionMatcher: Regex = Regions.values().map {
    _.getName
  }.sortBy {
    -1 * _.length
  }.mkString("|").r

  private class S3URLInfo(available: Boolean, contentLength: Long, lastModified: Long) extends URLHandler.URLInfo(available, contentLength, lastModified)

}

class S3URLHandler(s3Client: AmazonS3Client) extends URLHandler {

  import S3URLHandler._
  import org.apache.ivy.util.url.URLHandler.{ UNAVAILABLE, URLInfo }

  private def debug(msg: String): Unit = Message.debug("S3URLHandler." + msg)

  override def getURLInfo(url: URL): URLInfo = getURLInfo(url, 0)

  override def getURLInfo(url: URL, timeout: Int): URLInfo = try {
    debug(s"getURLInfo($url, $timeout)")

    val (bucket, key) = getClientBucketAndKey(url)

    val meta: ObjectMetadata = s3Client.getObjectMetadata(bucket, key)

    val available: Boolean = true
    val contentLength: Long = meta.getContentLength
    val lastModified: Long = meta.getLastModified.getTime

    new S3URLInfo(available, contentLength, lastModified)
  } catch {
    case ex: AmazonS3Exception if ex.getStatusCode == 404 => UNAVAILABLE
  }

  override def setRequestMethod(requestMethod: Int): Unit = debug(s"setRequestMethod($requestMethod)")

  override def openStream(url: URL): InputStream = {
    debug(s"openStream($url)")

    val (bucket, key) = getClientBucketAndKey(url)
    val obj: S3Object = s3Client.getObject(bucket, key)
    obj.getObjectContent
  }

  override def upload(src: File, dest: URL, l: CopyProgressListener): Unit = {
    debug(s"upload($src, $dest)")

    val event: CopyProgressEvent = new CopyProgressEvent()
    if (null != l) l.start(event)

    val (bucket, key) = getClientBucketAndKey(dest)
    val res: PutObjectResult = s3Client.putObject(bucket, key, src)

    if (null != l) l.end(event)
  }

  override def getLastModified(url: URL): Long = getURLInfo(url).getLastModified

  override def getLastModified(url: URL, timeout: Int): Long = getURLInfo(url, timeout).getLastModified

  private def getAmazonS3URI(uri: String): Option[AmazonS3URI] = getAmazonS3URI(URI.create(uri))

  private def getAmazonS3URI(url: URL): Option[AmazonS3URI] = getAmazonS3URI(url.toURI)

  private def getAmazonS3URI(uri: URI): Option[AmazonS3URI] = try {
    val httpsURI: URI =
      // If there is no scheme (e.g. new URI("s3-us-west-2.amazonaws.com/<bucket>"))
      // then we need to re-create the URI to add one and to also make sure the host is set
      if (uri.getScheme == null) new URI("https://" + uri)
      // AmazonS3URI can't parse the region from s3:// URLs so we rewrite the scheme to https://
      else new URI("https", uri.getUserInfo, uri.getHost, uri.getPort, uri.getPath, uri.getQuery, uri.getFragment)

    Some(new AmazonS3URI(httpsURI))
  } catch {
    case _: IllegalArgumentException => None
  }

  private def getBucketAndKey(url: URL): (String, String) = {
    // The AmazonS3URI constructor should work for standard S3 urls.  But if a custom domain is being used
    // (e.g. snapshots.maven.frugalmechanic.com) then we treat the hostname as the bucket and the path as the key
    getAmazonS3URI(url).map { amzn: AmazonS3URI =>
      (amzn.getBucket, amzn.getKey)
    }.getOrElse {
      // Probably a custom domain name - The host should be the bucket and the path the key
      (url.getHost, url.getPath.stripPrefix("/"))
    }
  }

  private def getRegionNameFromURL(url: URL): Option[String] = {
    // We'll try the AmazonS3URI parsing first then fallback to our RegionMatcher
    getAmazonS3URI(url).map {
      _.getRegion
    }.flatMap {
      Option(_)
    } orElse RegionMatcher.findFirstIn(url.toString)
  }

  private def getRegionNameFromDNS(bucket: String): Option[String] = {
    // This gives us something like s3-us-west-2-w.amazonaws.com which must have changed
    // at some point because the region from that hostname is no longer parsed by AmazonS3URI
    val canonicalHostName: String = InetAddress.getByName(bucket + ".s3.amazonaws.com").getCanonicalHostName

    // So we use our regex based RegionMatcher to try and extract the region since AmazonS3URI doesn't work
    RegionMatcher.findFirstIn(canonicalHostName)
  }

  // TODO: cache the result of this so we aren't always making the call
  private def getRegionNameFromService(bucket: String, client: AmazonS3Client): Option[String] = {
    // This might fail if the current credentials don't have access to the getBucketLocation call
    Try {
      client.getBucketLocation(bucket)
    }.toOption
  }

  def getClientBucketAndKey(url: URL): (String, String) = {
    val (bucket, key) = getBucketAndKey(url)
    (bucket, key)
  }

  override def download(src: URL, dest: File, l: CopyProgressListener): Unit = {
    debug(s"download($src, $dest)")

    val (bucket, key) = getClientBucketAndKey(src)

    val event: CopyProgressEvent = new CopyProgressEvent()
    if (null != l) l.start(event)

    val meta: ObjectMetadata = s3Client.getObject(new GetObjectRequest(bucket, key), dest)
    dest.setLastModified(meta.getLastModified.getTime)

    if (null != l) l.end(event) //l.progress(evt.update(EMPTY_BUFFER, 0, meta.getContentLength))
  }

  override def getContentLength(url: URL): Long = getURLInfo(url).getContentLength

  override def getContentLength(url: URL, timeout: Int): Long = getURLInfo(url, timeout).getContentLength

  override def isReachable(url: URL): Boolean = getURLInfo(url).isReachable

  override def isReachable(url: URL, timeout: Int): Boolean = getURLInfo(url, timeout).isReachable

}

package com.chatwork.sbt.aws.s3.resolver

import java.util.Collections

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{ CannedAccessControlList, Region }
import com.chatwork.sbt.aws.s3.resolver.ivy.S3IvyResolver
import sbt.Resolver

import scala.collection.JavaConverters._

object S3ResolverCreator {

  def create(s3Client: AmazonS3Client,
             region: Region,
             name: String,
             location: String,
             acl: CannedAccessControlList = CannedAccessControlList.PublicRead,
             serverSideEncryption: Boolean = false,
             overwrite: Boolean = false,
             m2compatible: Boolean = true): S3IvyResolver = {
    require(null != location && location != "", "Empty Location!")
    val pattern = Collections.singletonList(resolvePattern(location, Resolver.mavenStyleBasePattern)).asScala
    S3IvyResolver(s3Client, region, name, location, pattern, acl, serverSideEncryption, overwrite, m2compatible)
  }

  private def resolvePattern(base: String, pattern: String): String = {
    val normBase = base.replace('\\', '/')
    if (normBase.endsWith("/") || pattern.startsWith("/")) normBase + pattern else normBase + "/" + pattern
  }

}

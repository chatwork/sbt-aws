package com.chatwork.sbt.aws.s3.resolver.ivy

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{CannedAccessControlList, Region}
import com.chatwork.sbt.aws.s3.resolver.S3Utility
import org.apache.ivy.plugins.resolver.IBiblioResolver
import sbt.Resolver

import scala.collection.JavaConverters._

case class S3IvyResolver(s3Client: AmazonS3,
                         region: Region,
                         name: String,
                         location: String,
                         acl: CannedAccessControlList,
                         serverSideEncryption: Boolean,
                         overwrite: Boolean,
                         isMavenStyle: Boolean)
    extends IBiblioResolver {

  private def withBase(p: String): String =
    location.stripSuffix("/") + "/" + p.stripPrefix("/")

  setName(name)
  setRoot(location)
  setRepository(
    S3Repository(s3Client,
                 region,
                 S3Utility.getBucket(location),
                 acl,
                 serverSideEncryption,
                 overwrite))
  setM2compatible(isMavenStyle)

  if (isMavenStyle) {
    val patterns = Resolver.mavenStylePatterns.artifactPatterns.map(withBase)
    setArtifactPatterns(patterns.asJava)
  } else {
    val patterns = Resolver.ivyStylePatterns.ivyPatterns.map(withBase)
    setIvyPatterns(patterns.asJava)
  }

}

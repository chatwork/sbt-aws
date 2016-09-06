package com.chatwork.sbt.aws.s3.resolver.ivy

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{ CannedAccessControlList, Region }
import org.apache.ivy.plugins.resolver.IBiblioResolver
import sbt.{ Patterns, Resolver }
import scala.collection.JavaConverters._

case class S3IvyResolver(s3Client: AmazonS3Client,
                         region: Region,
                         name: String,
                         location: String,
                         acl: CannedAccessControlList,
                         serverSideEncryption: Boolean,
                         overwrite: Boolean,
                         isMavenStyle: Boolean) extends IBiblioResolver {
  setName(name)
  setRoot(location)
  setRepository(S3Repository(s3Client, region, acl, serverSideEncryption, overwrite))

  def withBase(p: String): String =
    location.stripSuffix("/") + "/" + p.stripPrefix("/")

  setM2compatible(isMavenStyle)

  if (isMavenStyle) {
    setArtifactPatterns(Resolver.mavenStylePatterns.artifactPatterns.map(withBase).asJava)
  } else {
    setIvyPatterns(Resolver.ivyStylePatterns.ivyPatterns.map(withBase).asJava)
  }

}

package com.chatwork.sbt.aws.s3.resolver.ivy

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{ CannedAccessControlList, Region }
import org.apache.ivy.plugins.resolver.IBiblioResolver
import sbt.{ Logger, Patterns, Resolver }

import scala.collection.JavaConverters._

case class S3IvyResolver(s3Client: AmazonS3Client,
                         region: Region,
                         name: String,
                         location: String,
                         patterns: Seq[String],
                         acl: CannedAccessControlList = CannedAccessControlList.PublicRead,
                         serverSideEncryption: Boolean = false,
                         overwrite: Boolean = false,
                         m2compatible: Boolean = true) extends IBiblioResolver {
  //logger.info("s:params = " + (name, location, acl, serverSideEncryption, overwrite, m2compatible))
  setName(name)
  setRoot(location)
  setM2compatible(m2compatible)
  setArtifactPatterns(patterns.asJava)
  setIvyPatterns(patterns.asJava)
  setRepository(S3Repository(s3Client, region, acl, serverSideEncryption, overwrite))
  //logger.info("e:params = " + (name, location, acl, serverSideEncryption, overwrite, m2compatible))

  def withPatterns(patterns: Patterns): S3IvyResolver = {
    if (patterns.isMavenCompatible) this.setM2compatible(true)

    def withBase(p: String): String = location.toString.stripSuffix("/") + "/" + p.stripPrefix("/")

    patterns.ivyPatterns.foreach { p => this.addIvyPattern(withBase(p)) }
    patterns.artifactPatterns.foreach { p => this.addArtifactPattern(withBase(p)) }

    this
  }

  def withIvyPatterns: S3IvyResolver = withPatterns(Resolver.ivyStylePatterns)
  def withMavenPatterns: S3IvyResolver = withPatterns(Resolver.mavenStylePatterns)

}

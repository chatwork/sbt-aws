package com.chatwork.sbt.aws.s3.resolver

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{CannedAccessControlList, Region}
import com.chatwork.sbt.aws.s3.resolver.ivy.S3IvyResolver
import sbt.{RawRepository, Resolver}

object ResolverCreator {

  def create(s3Client: AmazonS3,
             region: Region,
             name: String,
             location: String,
             acl: CannedAccessControlList,
             serverSideEncryption: Boolean,
             overwrite: Boolean,
             isMavenStyle: Boolean): Resolver = {
    new RawRepository(
      S3IvyResolver(
        s3Client,
        region,
        name,
        location,
        acl,
        serverSideEncryption,
        overwrite,
        isMavenStyle
      )
    )
  }

}

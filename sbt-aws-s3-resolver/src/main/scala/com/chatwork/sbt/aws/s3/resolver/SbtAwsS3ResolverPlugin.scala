package com.chatwork.sbt.aws.s3.resolver

import java.net.{ URL, URLConnection, URLStreamHandler }

import com.amazonaws.services.s3.AmazonS3Client
import com.chatwork.sbt.aws.core.SbtAwsCoreKeys
import com.chatwork.sbt.aws.s3.{ SbtAwsS3Keys, SbtAwsS3Plugin }
import com.chatwork.sbt.aws.s3.resolver.ivy.S3IvyResolver
import sbt.Keys._
import sbt.{ AutoPlugin, Plugins, Resolver }

object SbtAwsS3ResolverPlugin extends AutoPlugin with SbtAwsS3Resolver {

  override def trigger = allRequirements

  override def requires: Plugins = SbtAwsS3Plugin

  object autoImport extends SbtAwsS3ResolverKeys {

    object DeployStyle extends Enumeration {
      val Maven, Ivy2 = Value
    }

  }

  import SbtAwsCoreKeys._
  import SbtAwsS3Keys._

  import autoImport._

  override def projectSettings: Seq[_root_.sbt.Def.Setting[_]] = Seq(
    s3Region in aws := com.amazonaws.services.s3.model.Region.AP_Tokyo,
    s3DeployStyle in aws := DeployStyle.Maven,
    s3ServerSideEncryption in aws := false,
    s3Acl in aws := com.amazonaws.services.s3.model.CannedAccessControlList.PublicRead,
    s3OverwriteObject in aws := isSnapshot.value,
    s3Resolver in aws := { (name: String, location: String) =>
      val cpc = (credentialsProviderChain in aws).value
      val regions = (region in aws).value
      val _s3Region = (s3Region in aws).value
      val acl = (s3Acl in aws).value
      val sse = (s3ServerSideEncryption in aws).value
      val overwrite = (s3OverwriteObject in aws).value
      val deployStyle = (s3DeployStyle in aws).value
      val s3Client = createClient(cpc, classOf[AmazonS3Client], com.amazonaws.regions.Region.getRegion(regions))
      s3Client.setEndpoint(s"https://s3-${_s3Region.toString}.amazonaws.com")
      ResolverCreator.create(
        s3Client,
        _s3Region,
        name,
        location,
        acl,
        sse,
        overwrite,
        isMavenStyle = if (deployStyle == DeployStyle.Maven) true else false
      )
    }
  )
}

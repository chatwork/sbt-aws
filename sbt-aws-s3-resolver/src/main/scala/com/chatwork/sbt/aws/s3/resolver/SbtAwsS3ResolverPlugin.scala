package com.chatwork.sbt.aws.s3.resolver

import java.net.{ URL, URLConnection, URLStreamHandler, URLStreamHandlerFactory }

import com.amazonaws.regions.{ Region, Regions }
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.Region
import com.chatwork.sbt.aws.core.{ SbtAwsCoreKeys, SbtAwsCorePlugin }
import com.chatwork.sbt.aws.s3.SbtAwsS3Keys._
import com.chatwork.sbt.aws.s3.SbtAwsS3Plugin
import com.chatwork.sbt.aws.s3.SbtAwsS3Plugin._
import com.chatwork.sbt.aws.s3.resolver.ivy.{ S3IvyResolver, S3URLHandler }
import org.apache.ivy.util.Message
import org.apache.ivy.util.url.{ URLHandlerDispatcher, URLHandlerRegistry }
import sbt.Keys._
import sbt.{ AutoPlugin, Def, Plugins, Resolver, Task }

object DummyURLStreamHandler extends URLStreamHandler {
  def openConnection(url: URL): URLConnection = {
    null
  }
}

object SbtAwsS3ResolverPlugin extends AutoPlugin with SbtAwsS3Resolver {

  override def trigger = allRequirements

  override def requires: Plugins = SbtAwsS3Plugin

  object autoImport extends SbtAwsS3ResolverKeys {

    object DeployStyle extends Enumeration {
      val Maven, Ivy = Value
    }

    implicit def toSbtResolver(s3r: S3IvyResolver): Resolver = {
      if (s3r.getIvyPatterns.isEmpty || s3r.getArtifactPatterns.isEmpty) {
        s3r withPatterns Resolver.defaultPatterns
      }
      new sbt.RawRepository(s3r)
    }


    import SbtAwsCoreKeys._

    case class S3(profileName: Option[String], name: Option[String] = None, location: Option[String] = None,
                  regions: com.amazonaws.regions.Regions = com.amazonaws.regions.Regions.DEFAULT_REGION,
                  s3Region: com.amazonaws.services.s3.model.Region = com.amazonaws.services.s3.model.Region.AP_Tokyo,
                  acl: com.amazonaws.services.s3.model.CannedAccessControlList = com.amazonaws.services.s3.model.CannedAccessControlList.PublicRead,
                  sse: Boolean = false,
                  overwrite: Boolean = false,
                  deployStyle: DeployStyle.Value = DeployStyle.Maven) {
      def withName(v: String) = copy(name = Some(v))
      def withLocation(location: String)= {
        val s3Client = createClient(classOf[AmazonS3Client], com.amazonaws.regions.Region.getRegion(regions), profileName)
        s3Client.setEndpoint(s"https://s3-${s3Region.toString}.amazonaws.com")
        S3ResolverCreator.create(
          s3Client,
          s3Region,
          name.get,
          location,
          acl,
          serverSideEncryption = sse,
          overwrite = true,
          m2compatible = if (deployStyle == DeployStyle.Maven) true else false
        )
      }
    }

  }

  import SbtAwsCoreKeys._
  import com.chatwork.sbt.aws.s3.SbtAwsS3Keys._
  import SbtAwsS3ResolverKeys._

  //  private def info(msg: String): Unit = Message.info(msg)
  //
  //  private object S3URLStreamHandlerFactory extends URLStreamHandlerFactory {
  //    def createURLStreamHandler(protocol: String): URLStreamHandler = protocol match {
  //      case "s3" => DummyURLStreamHandler
  //      case _ => null
  //    }
  //  }
  //
  //  private val dispatcher: URLHandlerDispatcher = URLHandlerRegistry.getDefault match {
  //    case dispatcher: URLHandlerDispatcher =>
  //      info("Using the existing Ivy URLHandlerDispatcher to handle s3:// URLs")
  //      dispatcher
  //    case default =>
  //      info("Creating a new Ivy URLHandlerDispatcher to handle s3:// URLs")
  //      val dispatcher: URLHandlerDispatcher = new URLHandlerDispatcher()
  //      dispatcher.setDefault(default)
  //      URLHandlerRegistry.setDefault(dispatcher)
  //      dispatcher
  //  }
  //
  //  try {
  //    new URL("s3://example.com")
  //    info("The s3:// URLStreamHandler is already installed")
  //  } catch {
  //    // This means we haven't installed the handler, so install it
  //    case _: java.net.MalformedURLException =>
  //      info("Installing the s3:// URLStreamHandler via java.net.URL.setURLStreamHandlerFactory")
  //      URL.setURLStreamHandlerFactory(S3URLStreamHandlerFactory)
  //  }

  import com.chatwork.sbt.aws.s3.resolver.SbtAwsS3ResolverPlugin.autoImport.{ DeployStyle, S3 }

  override def projectSettings: Seq[_root_.sbt.Def.Setting[_]] = Seq(
    s3Region in aws := {
      com.amazonaws.services.s3.model.Region.AP_Tokyo
    },
    s3DeployStyle in aws := DeployStyle.Maven,
    s3ServerSideEncryption in aws := false,
    s3Acl in aws := com.amazonaws.services.s3.model.CannedAccessControlList.PublicRead,
    s3Resolver in aws := { (name: String, location: String) =>
      S3(
        profileName = (credentialProfileName in aws).value,
        acl = (s3Acl in aws).value,
        sse = (s3ServerSideEncryption in aws).value,
        overwrite = (s3OverwriteObject in aws).value
      ).withName(name).withLocation(location)
    }
  )
}

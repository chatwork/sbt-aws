package com.github.j5ik2o.aws

import java.io.File

import com.amazonaws.AmazonWebServiceClient
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{AWSCredentialsProviderChain, EnvironmentVariableCredentialsProvider, InstanceProfileCredentialsProvider, SystemPropertiesCredentialsProvider}
import com.amazonaws.regions.Region
import com.amazonaws.services.elasticbeanstalk._
import com.amazonaws.services.elasticbeanstalk.model._
import com.amazonaws.services.s3._
import com.amazonaws.services.s3.model._
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.sisioh.aws4s.eb.Implicits._
import org.sisioh.aws4s.eb.model._
import org.sisioh.aws4s.s3.Implicits._
import sbt._
import Keys._
import AwsKeys._
import AwsKeys.S3Keys._
import AwsKeys.EBKeys._

import scala.util.{Failure, Success, Try}

object SbtAws extends SbtAwsS3 with SbtAwsEB {

  private[aws] def newCredentialsProvider(profileName: String) = {
    new AWSCredentialsProviderChain(
      new EnvironmentVariableCredentialsProvider(),
      new SystemPropertiesCredentialsProvider(),
      new ProfileCredentialsProvider(profileName),
      new InstanceProfileCredentialsProvider()
    )
  }

  private[aws] def createClient[A <: AmazonWebServiceClient](serviceClass: Class[A], region: Region, profileName: String): A = {
    region.createClient(serviceClass, newCredentialsProvider(profileName), null)
  }

  private[aws] def md5(file: File): String =
    DigestUtils.md5Hex(FileUtils.readFileToByteArray(file))


}

trait SbtAwsEB {
  this: SbtAws.type =>

  lazy val ebClient = Def.task {
    createClient(classOf[AWSElasticBeanstalkClient], Region.getRegion((region in aws).value), (credentialProfileName in aws).value)
  }

  def copyFilesToSourceBundle(files: Seq[(File, File)]) = {
    IO.copy(files)
  }

  def createSourceBundle(base: File, zipFile: File): Try[Unit] = Try {
    IO.zip(Path.allSubpaths(base), zipFile)
  }

  def createSourceBundleTask(): Def.Initialize[Task[Unit]] = Def.task {

  }

  def ebDeleteApplication(client: AWSElasticBeanstalkClient, applicationName: String): Try[Unit] = {
    client.describeApplicationsAsTry(
      DescribeApplicationsRequestFactory.create().withApplicationNames(applicationName)
    ).flatMap { result =>
      if (result.applications.nonEmpty) {
        client.deleteApplicationAsTry(
          new DeleteApplicationRequest(applicationName)
        )
      } else Success(())
    }
  }

  def ebDeleteApplicationTask(): Def.Initialize[Task[Unit]] = Def.task {
    ebDeleteApplication(ebClient.value, (ebApplicationName in aws).value).get
  }

  def ebDeleteApplicationVersion(client: AWSElasticBeanstalkClient, applicationName: String, versionLabel: String): Try[Unit] = {
    client.describeApplicationVersionsAsTry(
      DescribeApplicationVersionsRequestFactory
        .create()
        .withApplicationName(applicationName)
    ).flatMap { result =>
      if (!result.getApplicationVersions.isEmpty) {
        client.deleteApplicationVersionAsTry(
          DeleteApplicationVersionRequestFactory
            .create(applicationName, versionLabel)
        )
      } else Success()
    }
  }

  def ebDeleteApplicationVersionTask(): Def.Initialize[Task[Unit]] = Def.task {
    ebDeleteApplicationVersion(ebClient.value, (ebApplicationName in aws).value, (ebVersionLabel in aws).value).get
  }

  def ebDeleteTemplate(client: AWSElasticBeanstalkClient, applicationName: String,
                       ebConfigurationTemplates: Seq[EbConfigurationTemplate]): Try[Seq[Unit]] = {
    ebConfigurationTemplates.foldLeft(Try(Seq.empty[Unit])) { (result, template) =>
      for {
        r <- result
        applicationDesc <- client.describeApplicationsAsTry(
          DescribeApplicationsRequestFactory
            .create()
            .withApplicationNames(applicationName)
        )
        allDeletes <- applicationDesc.applications.foldLeft(Try(Seq.empty[Unit])) { (result1, application) =>
          for {
            r1 <- result1
            deletes <- application.configurationTemplates.foldLeft(Try(Seq.empty[Unit])) { (result2, template) =>
              for {
                r2 <- result2
                delete <- client.deleteConfigurationTemplateAsTry(
                  DeleteConfigurationTemplateRequestFactory
                    .create()
                    .withApplicationName(applicationName)
                    .withTemplateName(template)
                )
              } yield r2 :+ delete
            }
          } yield r1 ++ deletes
        }
      } yield {
        r ++ allDeletes
      }
    }
  }

  def ebDeleteTemplateTask(): Def.Initialize[Task[Try[Seq[Unit]]]] = Def.task {
    ebDeleteTemplate(
      ebClient.value,
      (ebApplicationName in aws).value,
      (ebTemplates in aws).value
    )
  }

  def ebCreateApplication(client: AWSElasticBeanstalkClient, applicationName: String, description: Option[String]): Try[ApplicationDescription] = {
    client.describeApplicationsAsTry(
      DescribeApplicationsRequestFactory.create().withApplicationNames(applicationName)
    ).flatMap { result =>
      if (result.getApplications.isEmpty) {
        client.createApplicationAsTry(
          CreateApplicationRequestFactory
            .create(applicationName)
            .withDescriptionOpt(description)
        ).map {
          _.applicationOpt.get
        }
      } else {
        client.updateApplicationAsTry(
          UpdateApplicationRequestFactory
            .create(applicationName)
            .withDescriptionOpt(description)
        ).map {
          _.applicationOpt.get
        }
      }
    }
  }

  def ebCreateApplicationTask(): Def.Initialize[Task[ApplicationDescription]] = Def.task {
    ebCreateApplication(ebClient.value, (ebApplicationName in aws).value, (ebApplicationDescription in aws).value).get
  }

  def ebCreateApplicationVersion(client: AWSElasticBeanstalkClient, applicationName: String, versionLabel: String): Try[ApplicationVersionDescription] = {
    client.describeApplicationVersionsAsTry(
      DescribeApplicationVersionsRequestFactory
        .create()
        .withApplicationName(applicationName)
    ).flatMap { result =>
      if (result.getApplicationVersions.isEmpty) {
        client.createApplicationVersionAsTry(
          CreateApplicationVersionRequestFactory
            .create(applicationName, versionLabel)
        ).map(_.applicationVersionOpt.get)
      } else {
        client.updateApplicationVersionAsTry(
          UpdateApplicationVersionRequestFactory
            .create(applicationName, versionLabel)
        ).map(_.applicationVersionOpt.get)
      }
    }
  }

  def ebCreateApplicationVersionTask(): Def.Initialize[Task[ApplicationVersionDescription]] = Def.task {
    ebCreateApplicationVersion(
      ebClient.value,
      (ebApplicationName in aws).value,
      (ebVersionLabel in aws).value
    ).get
  }

  def ebCreateTemplate(client: AWSElasticBeanstalkClient,
                       applicationName: String,
                       ebConfigurationTemplates: Seq[EbConfigurationTemplate]) = {
    ebConfigurationTemplates.map { template =>
      client.describeApplicationsAsTry(
        DescribeApplicationsRequestFactory
          .create()
          .withApplicationNames(applicationName)
      ).map { result =>
        result.applications
          .head.configurationTemplates
          .find(_ == template.name).get
      }.flatMap { result =>
        if (template.recreate) {
          for {
            _ <- client.deleteConfigurationTemplateAsTry(
              DeleteConfigurationTemplateRequestFactory
                .create()
                .withApplicationName(applicationName)
                .withTemplateName(template.name)
            )
            result <- client.createConfigurationTemplateAsTry(
              CreateConfigurationTemplateRequestFactory
                .create()
                .withApplicationName(applicationName)
                .withTemplateName(template.name)
                .withDescription(template.description)
                .withOptionSettings(template.optionSettings)
            )
          } yield {
            result.applicationNameOpt.get
          }
        } else {
          client.updateConfigurationTemplateAsTry(
            UpdateConfigurationTemplateRequestFactory
              .create()
              .withApplicationName(applicationName)
              .withTemplateName(template.name)
              .withDescription(template.description)
              .withOptionSettings(template.optionSettings)
          ).map(_.applicationNameOpt.get)
        }
      }.orElse {
        client.createConfigurationTemplateAsTry(
          CreateConfigurationTemplateRequestFactory
            .create()
            .withApplicationName(applicationName)
            .withTemplateName(template.name)
            .withDescription(template.description)
            .withOptionSettings(template.optionSettings)
        ).map(_.applicationNameOpt.get)
      }
    }
  }

  def ebCreateTemplateTask(): Def.Initialize[Task[Unit]] = Def.task {
    ebCreateTemplate(ebClient.value, (ebApplicationName in aws).value, (ebTemplates in aws).value)
  }

}

trait SbtAwsS3 {
  this: SbtAws.type =>

  lazy val s3Client = Def.task {
    createClient(classOf[AmazonS3Client], Region.getRegion((region in aws).value), (credentialProfileName in aws).value)
  }

  def s3ExistsS3Object(client: AmazonS3Client, bucketName: String, key: String): Try[Boolean] = {
    s3GetS3ObjectMetadata(client, bucketName, key).map(_.isDefined)
  }

  def s3GetS3ObjectMetadata(client: AmazonS3Client, bucketName: String, key: String): Try[Option[ObjectMetadata]] = {
    client.getObjectMetadataAsTry(bucketName, key).map(Some(_)).recoverWith {
      case ex: AmazonS3Exception if ex.getStatusCode() == 404 =>
        Success(None)
      case ex =>
        Failure(ex)
    }
  }

  def s3Upload(logger: Logger,
               client: AmazonS3Client,
               file: File,
               bucketName: String,
               key: String,
               overwrite: Boolean,
               objectMetadataOpt: Option[ObjectMetadata]): Try[Option[String]] = {
    s3GetS3ObjectMetadata(client, bucketName, key).flatMap { metadataOpt =>
      val hash = md5(file)
      val metadataHash = metadataOpt.get.getETag
      logger.debug("file md5 = " + hash)
      logger.debug("metadata etag = " + metadataHash)
      if (metadataOpt.isEmpty || (overwrite && metadataHash != hash)) {
        (for {
          result <- client.putObjectAsTry(
            objectMetadataOpt.fold(new PutObjectRequest(bucketName, key, file)) { om =>
              new PutObjectRequest(bucketName, key, file).withMetadata(om)
            }
          )
          resourceUrl <- client.getResourceUrlAsTry(bucketName, key)
        } yield Some(resourceUrl)).map {
          url =>
            logger.info("uploaded file: url = " + url)
            url
        }
      } else {
        Success(None)
      }
    }
  }

  def s3UploadTask: Def.Initialize[Task[Option[String]]] = Def.task {
    s3Upload(
      streams.value.log,
      s3Client.value,
      (s3File in aws).value,
      (s3BucketName in aws).value,
      (s3Key in aws).value,
      (s3OverwriteObject in aws).value,
      (s3ObjectMetadata in aws).value
    ).get
  }

}

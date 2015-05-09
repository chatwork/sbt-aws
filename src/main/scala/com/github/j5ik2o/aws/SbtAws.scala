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
import com.github.j5ik2o.aws.AwsKeys.EBKeys._
import com.github.j5ik2o.aws.AwsKeys.S3Keys._
import com.github.j5ik2o.aws.AwsKeys._
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.sisioh.aws4s.eb.Implicits._
import org.sisioh.aws4s.s3.Implicits._
import sbt.Keys._
import sbt._

import scala.util.{Success, Try}

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

  private[aws] def exists(client: AmazonS3Client, bucketName: String, key: String): Boolean = {
    client.getObjectMetadataAsTry(bucketName, key).map(_ => true).recover {
      case ex: AmazonS3Exception if ex.getStatusCode == 404 => true
      case ex: AmazonS3Exception => false
    }.get
  }

  private[aws] def existingObjectMetadata(client: AmazonS3Client, bucketName: String, key: String): Option[ObjectMetadata] =
    client.getObjectMetadataAsTry(bucketName, key).toOption

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

  def deleteApplication(client: AWSElasticBeanstalkClient, applicationName: String): Try[Unit] = {
    client.describeApplicationsAsTry(
      new DescribeApplicationsRequest().withApplicationNames(applicationName)
    ).flatMap { result =>
      if (result.applications.nonEmpty) {
        client.deleteApplicationAsTry(
          new DeleteApplicationRequest(applicationName)
        )
      } else Success(())
    }
  }

  def deleteApplicationTask(): Def.Initialize[Task[Unit]] = Def.task {
    deleteApplication(ebClient.value, (ebApplicationName in aws).value).get
  }

  def createApplication(client: AWSElasticBeanstalkClient, applicationName: String, description: Option[String]): Try[ApplicationDescription] = {
    client.describeApplicationsAsTry(
      new DescribeApplicationsRequest().withApplicationNames(applicationName)
    ).flatMap { result =>
      if (result.getApplications.isEmpty) {
        client.createApplicationAsTry(
          new CreateApplicationRequest(applicationName)
            .withDescriptionOpt(description)
        ).map {
          _.applicationOpt.get
        }
      } else {
        client.updateApplicationAsTry(
          new UpdateApplicationRequest(applicationName)
            .withDescriptionOpt(description)
        ).map {
          _.applicationOpt.get
        }
      }
    }
  }

  def createApplicationTask(): Def.Initialize[Task[ApplicationDescription]] = Def.task {
    createApplication(ebClient.value, (ebApplicationName in aws).value, (ebApplicationDescription in aws).value).get
  }

  def createApplicationVersion(client: AWSElasticBeanstalkClient, applicationName: String, versionLabel: String): Try[ApplicationVersionDescription] = {
    client.describeApplicationVersionsAsTry(
      new DescribeApplicationVersionsRequest().withApplicationName(applicationName)
    ).flatMap { result =>
      if (result.getApplicationVersions.isEmpty) {
        client.createApplicationVersionAsTry(
          new CreateApplicationVersionRequest(applicationName, versionLabel)
        ).map(_.applicationVersionOpt.get)
      } else {
        client.updateApplicationVersionAsTry(
          new UpdateApplicationVersionRequest(applicationName, versionLabel)
        ).map(_.applicationVersionOpt.get)
      }
    }
  }

  def createApplicationVersionTask(): Def.Initialize[Task[ApplicationVersionDescription]] = Def.task {
    createApplicationVersion(ebClient.value, (ebApplicationName in aws).value, (ebVersionLabel in aws).value).get
  }

  def createTemplate(client: AWSElasticBeanstalkClient,
                     applicationName: String,
                     ebConfigurationTemplates: Seq[EbConfigurationTemplate]) = {
    ebConfigurationTemplates.map { template =>
      client.describeApplicationsAsTry(
        new DescribeApplicationsRequest()
          .withApplicationNames(applicationName)
      ).map { result =>
        result.applications
          .head.configurationTemplates
          .find(_ == template.name).get
      }.flatMap { result =>
        if (template.recreate) {
          for {
            _ <- client.deleteConfigurationTemplateAsTry(
              new DeleteConfigurationTemplateRequest()
                .withApplicationName(applicationName)
                .withTemplateName(template.name)
            )
            result <- client.createConfigurationTemplateAsTry(
              new CreateConfigurationTemplateRequest()
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
            new UpdateConfigurationTemplateRequest()
              .withApplicationName(applicationName)
              .withTemplateName(template.name)
              .withDescription(template.description)
              .withOptionSettings(template.optionSettings)
          ).map(_.applicationNameOpt.get)
        }
      }.orElse {
        client.createConfigurationTemplateAsTry(
          new CreateConfigurationTemplateRequest()
            .withApplicationName(applicationName)
            .withTemplateName(template.name)
            .withDescription(template.description)
            .withOptionSettings(template.optionSettings)
        ).map(_.applicationNameOpt.get)
      }
    }
  }

  def createTemplateTask(): Def.Initialize[Task[Unit]] = Def.task {
    createTemplate(ebClient.value, (ebApplicationName in aws).value, (ebTemplates in aws).value)
  }

}

trait SbtAwsS3 {
  this: SbtAws.type =>

  lazy val s3Client = Def.task {
    createClient(classOf[AmazonS3Client], Region.getRegion((region in aws).value), (credentialProfileName in aws).value)
  }

  def s3Upload(logger: Logger,
               client: AmazonS3Client,
               file: File,
               bucketName: String,
               key: String,
               overwrite: Boolean,
               objectMetadataOpt: Option[ObjectMetadata]): Try[Option[String]] = {
    val metadata = existingObjectMetadata(client, bucketName, key)
    if (metadata.isEmpty || (overwrite && metadata.get.getETag != md5(file))) {
      for {
        result <- client.putObjectAsTry(
          objectMetadataOpt.map { om =>
            new PutObjectRequest(bucketName, key, file)
              .withMetadata(om)
          }.getOrElse(
              new PutObjectRequest(bucketName, key, file)
            )
        )
        resourceUrl <- client.getResourceUrlAsTry(bucketName, key)
      } yield Some(resourceUrl)
    } else {
      Success(None)
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

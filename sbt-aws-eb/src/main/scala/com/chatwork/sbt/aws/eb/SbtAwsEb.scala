package com.chatwork.sbt.aws.eb

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

import com.amazonaws.regions.Region
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient
import com.amazonaws.services.elasticbeanstalk.model.{ ApplicationDescription, ApplicationVersionDescription, DeleteApplicationRequest }
import com.chatwork.sbt.aws.core.SbtAwsCoreKeys._
import com.chatwork.sbt.aws.eb.SbtAwsEbKeys._
import com.chatwork.sbt.aws.s3.SbtAwsS3
import org.sisioh.aws4s.eb.Implicits._
import org.sisioh.aws4s.eb.model._
import sbt.Keys._
import sbt._

import scala.util.{ Success, Try }

object SbtAwsEb extends SbtAwsEb

trait SbtAwsEb extends SbtAwsS3 {

  lazy val ebClient = Def.task {
    createClient(classOf[AWSElasticBeanstalkClient], Region.getRegion((region in aws).value), (credentialProfileName in aws).value)
  }

  def ebBuildBundleTask(): Def.Initialize[Task[File]] = Def.task {
    val logger = streams.value.log
    val files = (ebBundleTargetFiles in aws).value
    val path = baseDirectory.value / "target" / (ebBundleFileName in aws).value
    logger.info(s"create application-bundle: $path")
    IO.zip(files, path)
    logger.info(s"created application-bundle: $path")
    path
  }

  def ebUploadBundleTask(): Def.Initialize[Task[Unit]] = Def.task {
    val logger = streams.value.log
    val path = (ebBuildBundle in aws).value
    val createBucket = (ebS3CreateBucket in aws).value

    val projectName = (name in thisProjectRef).value
    val projectVersion = (version in thisProjectRef).value

    val sdf = new SimpleDateFormat("yyyyMMdd'_'HHmmss")
    val timestamp = sdf.format(new Date())

    val bucketName = (ebS3BucketName in aws).value
    val keyMapper = (ebS3KeyMapper in aws).value

    val baseKey = s"$projectName/$projectName-$projectVersion-$timestamp.zip"
    val key = keyMapper(baseKey)

    val overwrite = projectVersion.endsWith("-SNAPSHOT")

    logger.info(s"upload application-bundle : $path to $bucketName/$key")
    s3PutObject(s3Client.value, bucketName, key, path, overwrite, createBucket).get
    logger.info(s"uploaded application-bundle : $bucketName/$key")
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

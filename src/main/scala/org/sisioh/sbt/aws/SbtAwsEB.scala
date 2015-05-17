package org.sisioh.sbt.aws

import java.io.File

import com.amazonaws.regions.Region
import com.amazonaws.services.elasticbeanstalk._
import com.amazonaws.services.elasticbeanstalk.model._
import AwsKeys.EBKeys._
import AwsKeys._
import org.sisioh.aws4s.eb.Implicits._
import org.sisioh.aws4s.eb.model._
import sbt._

import scala.util.{ Success, Try }

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
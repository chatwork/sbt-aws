package com.chatwork.sbt.aws.eb

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

import com.amazonaws.regions.Region
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient
import com.amazonaws.services.elasticbeanstalk.model.{ ApplicationDescription, ApplicationVersionDescription, DeleteApplicationRequest, EnvironmentTier }
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
    val logger = streams.value.log
    val r = (region in aws).value
    val cpn = (credentialProfileName in aws).value
    logger.info(s"region = $r, credentialProfileName = $cpn")
    createClient(classOf[AWSElasticBeanstalkClient], Region.getRegion(r), cpn)
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
    val bucketName = (ebS3BucketName in aws).value
    val keyMapper = (ebS3KeyMapper in aws).value

    require(bucketName.isDefined)

    val sdf = new SimpleDateFormat("yyyyMMdd'_'HHmmss")
    val timestamp = sdf.format(new Date())

    val baseKey = s"$projectName/$projectName-$projectVersion-$timestamp.zip"
    val key = keyMapper(baseKey)

    val overwrite = projectVersion.endsWith("-SNAPSHOT")

    logger.info(s"upload application-bundle : $path to ${bucketName.get}/$key")
    s3PutObject(s3Client.value, bucketName.get, key, path, overwrite, createBucket).get
    logger.info(s"uploaded application-bundle : ${bucketName.get}/$key")
  }

  private[eb] def existsApplication(client: AWSElasticBeanstalkClient, applicationName: String, updateAt: Option[Date] = None): Try[Boolean] = {
    val request = DescribeApplicationsRequestFactory.create().withApplicationNames(applicationName)
    client.describeApplicationsAsTry(request).map { result =>
      result.applications.headOption.exists { e =>
        e.applicationNameOpt.exists(_ == applicationName) && updateAt.fold(true) { ut => e.getDateUpdated.getTime > ut.getTime }
      }
    }
  }

  private[eb] def waitApplication(client: AWSElasticBeanstalkClient,
                                  applicationName: String, updateAt: Option[Date], exists: Boolean)(implicit logger: Logger) = {
    def statuses: Stream[Boolean] = Stream.cons(existsApplication(client, applicationName).get, statuses)

    val progressStatuses: Stream[Boolean] = statuses.takeWhile { status => status != exists }
    (progressStatuses, () => statuses.headOption)
  }

  private[eb] def describeApplication(client: AWSElasticBeanstalkClient, applicationName: String): Try[Option[ApplicationDescription]] = {
    client.describeApplicationsAsTry(
      DescribeApplicationsRequestFactory
        .create()
        .withApplicationNames(applicationName)
    ).map(_.applications.find(_.getApplicationName == applicationName))
  }

  private[eb] def ebCreateApplication(client: AWSElasticBeanstalkClient, applicationName: String, description: Option[String])(implicit logger: Logger): Try[ApplicationDescription] = {
    val result = describeApplication(client, applicationName).flatMap { result =>
      if (result.isEmpty) {
        logger.info(s"create application $applicationName, $description")
        val result = client.createApplicationAsTry(
          CreateApplicationRequestFactory
            .create(applicationName)
            .withDescriptionOpt(description)
        ).map {
            _.applicationOpt.get
          }
        logger.info(s"created application $applicationName, $description")
        result
      } else {
        throw new Exception
      }
    }
    result
  }

  def ebCreateApplicationTask(): Def.Initialize[Task[ApplicationDescription]] = Def.task {
    implicit val logger = streams.value.log
    ebCreateApplication(
      ebClient.value,
      (ebApplicationName in aws).value,
      (ebApplicationDescription in aws).value
    ).get
  }

  def ebCreateApplicationAndWaitTask(): Def.Initialize[Task[ApplicationDescription]] = Def.task {
    implicit val logger = streams.value.log
    val app = (ebApplicationCreate in aws).value
    val (progressStatuses, headOption) = waitApplication(ebClient.value, app.applicationNameOpt.get, None, true)
    progressStatuses.foreach { s =>
      logger.info(s"status = $s")
      Thread.sleep(500)
    }
    headOption()
    app
  }

  private[eb] def ebUpdateApplication(client: AWSElasticBeanstalkClient, applicationName: String, description: Option[String])(implicit logger: Logger): Try[ApplicationDescription] = {
    describeApplication(client, applicationName).flatMap { result =>
      if (result.isDefined) {
        logger.info(s"update application $applicationName, $description")
        val result = client.updateApplicationAsTry(
          UpdateApplicationRequestFactory
            .create(applicationName)
            .withDescriptionOpt(description)
        ).map {
            _.applicationOpt.get
          }
        logger.info(s"updated application $applicationName, $description")
        result
      } else {
        throw new Exception
      }
    }
  }

  def ebUpdateApplicationTask(): Def.Initialize[Task[ApplicationDescription]] = Def.task {
    implicit val logger = streams.value.log
    ebUpdateApplication(
      ebClient.value,
      (ebApplicationName in aws).value,
      (ebApplicationDescription in aws).value
    ).get
  }

  def ebUpdateApplicationAndWaitTask(): Def.Initialize[Task[ApplicationDescription]] = Def.task {
    implicit val logger = streams.value.log
    describeApplication(ebClient.value, (ebApplicationName in aws).value).map { result =>
      val app = (ebApplicationUpdate in aws).value
      val (progressStatuses, headOption) = waitApplication(ebClient.value, app.applicationNameOpt.get, Some(app.getDateUpdated), true)
      progressStatuses.foreach { s =>
        logger.info(s"status = $s")
        Thread.sleep(500)
      }
      headOption()
      app
    }.get
  }

  private[eb] def ebDeleteApplication(client: AWSElasticBeanstalkClient, applicationName: String)(implicit logger: Logger): Try[Unit] = {
    describeApplication(client, applicationName).flatMap { result =>
      if (result.isEmpty) {
        logger.info(s"delete application $applicationName")
        val result = client.deleteApplicationAsTry(
          new DeleteApplicationRequest(applicationName)
        )
        logger.info(s"deleted application $applicationName")
        result
      } else {
        logger.warn(s"not found $applicationName")
        Success(())
      }
    }
  }

  def ebDeleteApplicationTask(): Def.Initialize[Task[Unit]] = Def.task {
    implicit val logger = streams.value.log
    ebDeleteApplication(
      ebClient.value,
      (ebApplicationName in aws).value
    ).get
  }

  def ebDeleteApplicationAndWaitTask(): Def.Initialize[Task[Unit]] = Def.task {
    implicit val logger = streams.value.log
    val app = (ebApplicationDelete in aws).value
    val (progressStatuses, headOption) = waitApplication(ebClient.value, (ebApplicationName in aws).value, None, false)
    progressStatuses.foreach { s =>
      logger.info(s"status = $s")
      Thread.sleep(500)
    }
    headOption()
    app
  }

  private[eb] def ebCreateOrUpdateApplication(client: AWSElasticBeanstalkClient, applicationName: String, description: Option[String])(implicit logger: Logger): Try[ApplicationDescription] = {
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

  private[eb] def ebCreateOrUpdateApplicationTask(): Def.Initialize[Task[ApplicationDescription]] = Def.task {
    implicit val logger = streams.value.log
    ebCreateOrUpdateApplication(
      ebClient.value,
      (ebApplicationName in aws).value,
      (ebApplicationDescription in aws).value
    ).get
  }

  private[eb] def ebCreateOrUpdateApplicationAndWaitTask(): Def.Initialize[Task[ApplicationDescription]] = Def.task {
    implicit val logger = streams.value.log
    val app = (ebApplicationCreateOrUpdateAndWait in aws).value
    val (progressStatuses, headOption) = waitApplication(ebClient.value, (ebApplicationName in aws).value, Some(app.getDateUpdated), true)
    progressStatuses.foreach { s =>
      logger.info(s"status = $s")
      Thread.sleep(500)
    }
    headOption()
    app
  }

  def ebCreateEnvironment(client: AWSElasticBeanstalkClient,
                          applicationName: String,
                          environmentName: String,
                          description: Option[String],
                          tier: EnvironmentTier,
                          solutionStackName: String,
                          templateName: String,
                          versionLabel: String)(implicit logger: Logger): Try[String] = {
    val request = CreateEnvironmentRequestFactory.create()
      .withApplicationName(applicationName)
      .withEnvironmentName(environmentName)
      .withDescriptionOpt(description)
      .withTier(tier)
      .withSolutionStackName(solutionStackName)
      .withTemplateName(templateName)
      .withVersionLabel(versionLabel)
    client.createEnvironmentAsTry(request).map(_.environmentNameOpt.get)
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

  def ebDeleteConfigurationTemplate(client: AWSElasticBeanstalkClient, applicationName: String,
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

  def ebDeleteConfigurationTemplateTask(): Def.Initialize[Task[Try[Seq[Unit]]]] = Def.task {
    ebDeleteConfigurationTemplate(
      ebClient.value,
      (ebApplicationName in aws).value,
      (ebTemplates in aws).value
    )
  }

  def ebCreateApplicationVersion(client: AWSElasticBeanstalkClient, applicationName: String, versionLabel: String)(implicit logger: Logger): Try[ApplicationVersionDescription] = {
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
    implicit val logger = streams.value.log
    ebCreateApplicationVersion(
      ebClient.value,
      (ebApplicationName in aws).value,
      (ebVersionLabel in aws).value
    ).get
  }

  def ebCreateConfigurationTemplate(client: AWSElasticBeanstalkClient,
                                    applicationName: String,
                                    ebConfigurationTemplate: EbConfigurationTemplate)(implicit logger: Logger): Try[String] = {
    for {
      result <- client.createConfigurationTemplateAsTry(
        CreateConfigurationTemplateRequestFactory
          .create()
          .withApplicationName(applicationName)
          .withTemplateName(ebConfigurationTemplate.name)
          .withDescription(ebConfigurationTemplate.description)
          .withOptionSettings(ebConfigurationTemplate.optionSettings)
      )
    } yield {
      result.applicationNameOpt.get
    }
  }

  def ebUpdateConfigurationTemplate(client: AWSElasticBeanstalkClient,
                                    applicationName: String,
                                    ebConfigurationTemplate: EbConfigurationTemplate)(implicit logger: Logger): Try[String] = {
    for {
      result <- client.updateConfigurationTemplateAsTry(
        UpdateConfigurationTemplateRequestFactory
          .create()
          .withApplicationName(applicationName)
          .withTemplateName(ebConfigurationTemplate.name)
          .withDescription(ebConfigurationTemplate.description)
          .withOptionSettings(ebConfigurationTemplate.optionSettings))
    } yield {
      result.applicationNameOpt.get
    }
  }

  def ebDeleteConfigurationTemplate(client: AWSElasticBeanstalkClient,
                                    applicationName: String,
                                    ebConfigurationTemplate: EbConfigurationTemplate)(implicit logger: Logger): Try[Unit] = {
    for {
      result <- client.deleteConfigurationTemplateAsTry(
        DeleteConfigurationTemplateRequestFactory
          .create()
          .withApplicationName(applicationName)
          .withTemplateName(ebConfigurationTemplate.name)
      )
    } yield result
  }

  //  def waitAppilcation(client: AWSElasticBeanstalkClient,
  //                      applicationName: String)(implicit logger: Logger) = {
  //    def statuses: Stream[String] = Stream.cons(getApplicationStatus(client, applicationName).get.getOrElse(""), statuses)
  //
  //    val progressStatuses: Stream[String] = statuses.takeWhile { status =>
  //      logger.info(status)
  //      status.endsWith("_PROGRESS")
  //    }
  //    (progressStatuses, () => statuses.headOption)
  //  }

}

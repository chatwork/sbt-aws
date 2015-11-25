package com.chatwork.sbt.aws.eb

import java.util.concurrent.TimeUnit

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient
import com.amazonaws.services.elasticbeanstalk.model.{ ApplicationVersionDescription, S3Location }
import com.chatwork.sbt.aws.core.SbtAwsCoreKeys._
import com.chatwork.sbt.aws.eb.SbtAwsEbKeys._
import org.sisioh.aws4s.eb.Implicits._
import org.sisioh.aws4s.eb.model._
import sbt.Keys._
import sbt._

import scala.util.{ Success, Try }

trait ApplicationVersionSupport {
  this: SbtAwsEb =>

  private[eb] def describeApplicationVersion(client: AWSElasticBeanstalkClient, applicationName: String, versionLabel: String): Try[Option[ApplicationVersionDescription]] = {
    client.describeApplicationVersionsAsTry(
      DescribeApplicationVersionsRequestFactory
        .create()
        .withApplicationName(applicationName)
        .withVersionLabels(versionLabel)
    ).map(_.applicationVersions.find(e => e.getApplicationName == applicationName && e.getVersionLabel == versionLabel))
  }

  private[eb] def ebCreateApplicationVersion(client: AWSElasticBeanstalkClient,
                                             applicationName: String,
                                             versionLabel: String,
                                             versionDescription: Option[String] = None,
                                             s3Location: Option[S3Location] = None,
                                             autoCreateApplication: Option[Boolean] = None)(implicit logger: Logger): Try[ApplicationVersionDescription] = {
    val result = describeApplicationVersion(client, applicationName, versionLabel).flatMap { result =>
      if (result.isEmpty) {
        logger.info(s"create applicationVersion start: $applicationName, $versionLabel, $versionDescription, $s3Location, $autoCreateApplication")
        val request = CreateApplicationVersionRequestFactory
          .create()
          .withApplicationName(applicationName)
          .withVersionLabel(versionLabel)
          .withDescriptionOpt(versionDescription)
          .withAutoCreateApplicationOpt(autoCreateApplication)
          .withSourceBundleOpt(s3Location)
        val result = client.createApplicationVersionAsTry(request).map {
          _.applicationVersionOpt.get
        }
        logger.info(s"create applicationVersion finish: $applicationName, $versionLabel, $versionDescription, $s3Location, $autoCreateApplication")
        result
      } else {
        logger.warn(s"The applicationVersion is not found.: $applicationName, $versionLabel")
        throw new Exception
      }
    }
    result
  }

  def ebCreateApplicationVersionTask(): Def.Initialize[Task[ApplicationVersionDescription]] = Def.task {
    implicit val logger = streams.value.log
    val s3Location = if ((ebUseBundle in aws).value) {
      val (bucketName, key) = (ebUploadBundle in aws).value
      Some(S3LocationFactory.create().withS3Bucket(bucketName).withS3Key(key))
    } else None
    ebCreateApplicationVersion(
      ebClient.value,
      (ebApplicationName in aws).value,
      (ebApplicationVersionLabel in aws).value,
      (ebApplicationVersionDescription in aws).value,
      s3Location,
      (ebAutoCreateApplication in aws).value
    ).get
  }

  def ebCreateApplicationVersionAndWaitTask(): Def.Initialize[Task[ApplicationVersionDescription]] = Def.task {
    implicit val logger = streams.value.log
    val applicationVersion = (ebApplicationVersionCreate in aws).value
    val (progressStatuses, headOption) = wait(ebClient.value) {
      describeApplicationVersion(ebClient.value, applicationVersion.applicationNameOpt.get, applicationVersion.versionLabelOpt.get).get
    } {
      _.exists(e => e.applicationNameOpt == applicationVersion.applicationNameOpt && e.versionLabelOpt == applicationVersion.versionLabelOpt && e.descriptionOpt == applicationVersion.descriptionOpt)
    }
    progressStatuses.foreach { s =>
      logger.info(s"status = $s")
      TimeUnit.SECONDS.sleep(WAITING_INTERNALVAL_IN_SEC)
    }
    headOption().flatten.get
  }

  private[eb] def ebUpdateApplicationVersion(client: AWSElasticBeanstalkClient, applicationName: String, versionLabel: String, description: Option[String])(implicit logger: Logger): Try[ApplicationVersionDescription] = {
    client.describeApplicationVersionsAsTry(
      DescribeApplicationVersionsRequestFactory
        .create()
        .withApplicationName(applicationName)
    ).flatMap { applicationVersion =>
        if (!applicationVersion.getApplicationVersions.isEmpty) {
          logger.info(s"update applicationVersion start: $applicationName, $versionLabel")
          val result = client.updateApplicationVersionAsTry(
            UpdateApplicationVersionRequestFactory
              .create(applicationName, versionLabel)
              .withDescriptionOpt(description)
          )
          logger.info(s"update applicationVersion finish: $applicationName, $versionLabel")
          result.map(_.getApplicationVersion)
        } else {
          logger.warn(s"The applicationVersion is not found.: $applicationName, $versionLabel")
          throw new Exception
        }
      }
  }

  def ebUpdateApplicationVersionTask(): Def.Initialize[Task[ApplicationVersionDescription]] = Def.task {
    implicit val logger = streams.value.log
    ebUpdateApplicationVersion(ebClient.value,
      (ebApplicationName in aws).value,
      (ebApplicationVersionLabel in aws).value,
      (ebApplicationVersionDescription in aws).value
    ).get
  }

  def ebUpdateApplicationVersionAndWaitTask(): Def.Initialize[Task[ApplicationVersionDescription]] = Def.task {
    implicit val logger = streams.value.log
    val applicationVersion = (ebApplicationVersionUpdate in aws).value
    val (progressStatuses, headOption) = wait(ebClient.value) {
      describeApplicationVersion(ebClient.value, applicationVersion.applicationNameOpt.get, applicationVersion.versionLabelOpt.get).get
    } {
      _.exists(e => e.applicationNameOpt == applicationVersion.applicationNameOpt && e.versionLabelOpt == applicationVersion.versionLabelOpt)
    }
    progressStatuses.foreach { s =>
      logger.info(s"status = $s")
      TimeUnit.SECONDS.sleep(WAITING_INTERNALVAL_IN_SEC)
    }
    headOption().flatten.get
  }

  private[eb] def ebDeleteApplicationVersion(client: AWSElasticBeanstalkClient, applicationName: String, versionLabel: String)(implicit logger: Logger): Try[Unit] = {
    client.describeApplicationVersionsAsTry(
      DescribeApplicationVersionsRequestFactory
        .create()
        .withApplicationName(applicationName)
    ).flatMap { applicaitonVersison =>
        if (!applicaitonVersison.getApplicationVersions.isEmpty) {
          logger.info(s"delete applicationVersion start: $applicationName, $versionLabel")
          val result = client.deleteApplicationVersionAsTry(
            DeleteApplicationVersionRequestFactory
              .create(applicationName, versionLabel)
          )
          logger.info(s"delete applicationVersion finish: $applicationName, $versionLabel")
          result
        } else Success(())
      }
  }

  def ebDeleteApplicationVersionTask(): Def.Initialize[Task[Unit]] = Def.task {
    implicit val logger = streams.value.log
    ebDeleteApplicationVersion(
      ebClient.value,
      (ebApplicationName in aws).value,
      (ebApplicationVersionLabel in aws).value
    ).get
  }

  def ebDeleteApplicationVersionAndWaitTask(): Def.Initialize[Task[Unit]] = Def.task {
    implicit val logger = streams.value.log
    (ebApplicationVersionDelete in aws).value
    val (progressStatuses, headOption) = wait(ebClient.value) {
      describeApplicationVersion(ebClient.value, (ebApplicationName in aws).value, (ebApplicationVersionLabel in aws).value).get
    } {
      _.isEmpty
    }
    progressStatuses.foreach { s =>
      logger.info(s"status = $s")
      TimeUnit.SECONDS.sleep(WAITING_INTERNALVAL_IN_SEC)
    }
    headOption().flatten.get
  }

}

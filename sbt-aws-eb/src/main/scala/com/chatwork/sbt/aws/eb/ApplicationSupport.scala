package com.chatwork.sbt.aws.eb

import java.util.concurrent.TimeUnit

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient
import com.amazonaws.services.elasticbeanstalk.model.{ ApplicationDescription, CreateApplicationResult, UpdateApplicationResult }
import com.chatwork.sbt.aws.core.SbtAwsCoreKeys._
import com.chatwork.sbt.aws.eb.SbtAwsEbKeys._
import org.sisioh.aws4s.eb.Implicits._
import org.sisioh.aws4s.eb.model.{ CreateApplicationRequestFactory, DeleteApplicationRequestFactory, DescribeApplicationsRequestFactory, UpdateApplicationRequestFactory }
import sbt.Keys._
import sbt._

import scala.util.{ Success, Try }

trait ApplicationSupport {
  this: SbtAwsEb =>

  private[eb] def describeApplication(client: AWSElasticBeanstalkClient, applicationName: String)(implicit logger: Logger): Try[Option[ApplicationDescription]] = {
    client.describeApplicationsAsTry(
      DescribeApplicationsRequestFactory
        .create()
        .withApplicationNames(applicationName)
    ).map(_.applications.find(_.getApplicationName == applicationName))
  }

  private[eb] def ebCreateApplication(client: AWSElasticBeanstalkClient, applicationName: String, description: Option[String])(implicit logger: Logger): Try[ApplicationDescription] = {
    val result = describeApplication(client, applicationName).flatMap { applicationDescription =>
      if (applicationDescription.isEmpty) {
        logger.info(s"create application start: $applicationName, $description")
        val result = client.createApplicationAsTry(
          CreateApplicationRequestFactory
            .create(applicationName)
            .withDescriptionOpt(description)
        )
        logger.info(s"created application finish: $applicationName, $description")
        result.map(_.getApplication)
      } else {
        logger.warn(s"The application is already.: $applicationName")
        throw AlreadyException(s"The application is already.: $applicationName")
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
    val result = (ebApplicationCreate in aws).value
    val (progressStatuses, headOption) = wait(ebClient.value) {
      describeApplication(ebClient.value, result.getApplicationName).get
    } {
      _.exists(_.applicationNameOpt == result.applicationNameOpt)
    }
    progressStatuses.foreach { s =>
      logger.info(s"status = $s")
      TimeUnit.SECONDS.sleep(waitingIntervalInSec)
    }
    headOption().flatten.get
  }

  private[eb] def ebUpdateApplication(client: AWSElasticBeanstalkClient, applicationName: String, description: Option[String])(implicit logger: Logger): Try[ApplicationDescription] = {
    describeApplication(client, applicationName).flatMap { application =>
      if (application.isDefined) {
        logger.info(s"update application start: $applicationName, $description")
        if (!description.exists(_ == application.get.getDescription)) {
          val result = client.updateApplicationAsTry(
            UpdateApplicationRequestFactory
              .create(applicationName)
              .withDescriptionOpt(description)
          )
          logger.info(s"update application finish: $applicationName, $description")
          result.map(_.getApplication)
        } else {
          logger.warn(s"The Updating is nothing. $applicationName")
          Success(application.get)
        }
      } else {
        logger.warn(s"The application is not found.: $applicationName")
        throw NotFoundException(s"The application is not found.: $applicationName")
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
    val result = (ebApplicationUpdate in aws).value
    val (progressStatuses, headOption) = wait(ebClient.value) {
      describeApplication(ebClient.value, result.getApplicationName).get
    } {
      _.exists(e => e.applicationNameOpt == result.applicationNameOpt && e.descriptionOpt == result.descriptionOpt)
    }
    progressStatuses.foreach { s =>
      logger.info(s"status = $s")
      TimeUnit.SECONDS.sleep(waitingIntervalInSec)
    }
    headOption().flatten.get
  }

  private[eb] def ebDeleteApplication(client: AWSElasticBeanstalkClient, applicationName: String)(implicit logger: Logger): Try[Unit] = {
    describeApplication(client, applicationName).flatMap { result =>
      if (result.nonEmpty) {
        logger.info(s"delete application start: $applicationName")
        val result = client.deleteApplicationAsTry(
          DeleteApplicationRequestFactory.create(applicationName)
        )
        logger.info(s"delete application finish: $applicationName")
        result
      } else {
        logger.warn(s"The application is not found.: $applicationName")
        throw NotFoundException(s"The application is not found.: $applicationName")
      }
    }
  }

  def ebDeleteApplicationTask(): Def.Initialize[Task[Unit]] = Def.task {
    implicit val logger = streams.value.log
    ebDeleteApplication(
      ebClient.value,
      (ebApplicationName in aws).value
    ).recover {
        case ex: NotFoundException =>
          ()
      }.get
  }

  def ebDeleteApplicationAndWaitTask(): Def.Initialize[Task[Unit]] = Def.task {
    implicit val logger = streams.value.log
    (ebApplicationDelete in aws).value
    val (progressStatuses, _) = wait(ebClient.value) {
      describeApplication(ebClient.value, (ebApplicationName in aws).value).get
    } {
      _.isEmpty
    }
    progressStatuses.foreach { s =>
      logger.info(s"status = $s")
      TimeUnit.SECONDS.sleep(waitingIntervalInSec)
    }
  }

  private[eb] def ebCreateOrUpdateApplicationTask(): Def.Initialize[Task[ApplicationDescription]] = Def.task {
    implicit val logger = streams.value.log
    ebUpdateApplication(
      ebClient.value,
      (ebApplicationName in aws).value,
      (ebApplicationDescription in aws).value
    ).recoverWith {
        case ex: NotFoundException =>
          ebCreateApplication(
            ebClient.value,
            (ebApplicationName in aws).value,
            (ebApplicationDescription in aws).value
          )
      }.get
  }

  private[eb] def ebCreateOrUpdateApplicationAndWaitTask(): Def.Initialize[Task[ApplicationDescription]] = Def.task {
    implicit val logger = streams.value.log
    val application = (ebApplicationCreateOrUpdateAndWait in aws).value
    val (progressStatuses, headOption) = wait(ebClient.value) {
      describeApplication(ebClient.value, application.applicationNameOpt.get).get
    } {
      _.exists(e => e.applicationNameOpt == application.applicationNameOpt && e.descriptionOpt == application.descriptionOpt)
    }
    progressStatuses.foreach {
      s =>
        logger.info(s"status = $s")
        TimeUnit.SECONDS.sleep(waitingIntervalInSec)
    }
    headOption().flatten.get
  }

}

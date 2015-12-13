package com.chatwork.sbt.aws.eb

import java.util.concurrent.TimeUnit

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient
import com.amazonaws.services.elasticbeanstalk.model._
import com.chatwork.sbt.aws.core.SbtAwsCoreKeys._
import com.chatwork.sbt.aws.eb.SbtAwsEbKeys._
import org.sisioh.aws4s.eb.Implicits._
import org.sisioh.aws4s.eb.model._
import sbt.Keys._
import sbt._

import scala.util.Try

trait EnvironmentSupport {
  this: SbtAwsEb =>

  private[eb] def ebDescribeEnvironment(client: AWSElasticBeanstalkClient,
                                        applicationName: String,
                                        environmentName: String,
                                        versionLabel: Option[String])(implicit logger: Logger): Try[Option[EnvironmentDescription]] = {
    logger.info(s"describe environment start: $applicationName, $environmentName")
    val request = DescribeEnvironmentsRequestFactory
      .create()
      .withApplicationName(applicationName)
      .withEnvironmentIds(environmentName)
    val result = client.describeEnvironmentsAsTry(request)
    logger.info(result.toString)
    logger.info(s"describe environment start: $applicationName, $environmentName")
    result.map {
      _.environments.find {
        e =>
          e.applicationNameOpt.get == applicationName &&
            e.environmentNameOpt.get == environmentName &&
            e.versionLabelOpt == versionLabel
      }
    }
  }

  private[eb] def ebDescribeEnvironment(client: AWSElasticBeanstalkClient,
                                        environmentId: String)(implicit logger: Logger): Try[Option[EnvironmentDescription]] = {
    logger.info(s"describe environment start: $environmentId")
    val request = DescribeEnvironmentsRequestFactory
      .create()
      .withEnvironmentIds(environmentId)
    val result = client.describeEnvironmentsAsTry(request)
    logger.info(result.toString)
    logger.info(s"describe environment finish: $environmentId")
    result.map {
      _.environments.find {
        _.environmentIdOpt.get == environmentId
      }
    }
  }

  private[eb] def ebCreateEnvironment(client: AWSElasticBeanstalkClient,
                                      applicationName: String,
                                      environmentName: String,
                                      description: Option[String],
                                      versionLabel: Option[String],
                                      tier: Option[EnvironmentTier],
                                      solutionStackName: Option[String],
                                      configurationTemplateName: Option[String],
                                      configurationOptionSettings: Seq[ConfigurationOptionSetting],
                                      optionSpecifications: Seq[OptionSpecification],
                                      tags: Seq[Tag],
                                      cnamePrefix: Option[String])(implicit logger: Logger): Try[EnvironmentDescription] = {
    ebDescribeEnvironment(client, applicationName, environmentName, versionLabel).flatMap { environment =>
      if (environment.isEmpty) {
        logger.info(s"create environment start: $applicationName, $environmentName, $description, $versionLabel, $tier, $solutionStackName, $configurationTemplateName")
        val request = CreateEnvironmentRequestFactory
          .create()
          .withApplicationName(applicationName)
          .withEnvironmentName(environmentName)
          .withVersionLabelOpt(versionLabel)
          .withDescriptionOpt(description)
          .withTierOpt(tier)
          .withSolutionStackNameOpt(solutionStackName)
          .withTemplateNameOpt(configurationTemplateName)
          .withOptionSettings(configurationOptionSettings: _*)
          .withOptionsToRemove(optionSpecifications: _*)
          .withTags(tags: _*)
          .withCNAMEPrefixOpt(cnamePrefix)
        val result = client.createEnvironmentAsTry(request)
        logger.info(s"create environment finish: $applicationName, $environmentName, $description, $versionLabel, $tier, $solutionStackName, $configurationTemplateName")
        result.map { r =>
          EnvironmentDescriptionFactory
            .create()
            .withApplicationName(r.getApplicationName)
            .withCNAME(r.getCNAME)
            .withDateCreated(r.getDateCreated)
            .withDateUpdated(r.getDateUpdated)
            .withDescription(r.getDescription)
            .withEndpointURL(r.getEndpointURL)
            .withEnvironmentId(r.getEnvironmentId)
            .withEnvironmentName(r.getEnvironmentName)
            .withHealth(r.getHealth)
            .withResources(r.getResources)
            .withSolutionStackName(r.getSolutionStackName)
            .withStatus(r.getStatus)
            .withTemplateName(r.getTemplateName)
            .withTier(r.getTier)
            .withVersionLabel(r.getVersionLabel)
        }
      } else {
        logger.warn(s"exists environment: $applicationName, $environmentName")
        throw AlreadyException(s"already environment: $applicationName, $environmentName")
      }
    }
  }

  def ebCreateEnvironmentTask: Def.Initialize[Task[EnvironmentDescription]] = Def.task {
    implicit val logger = streams.value.log
    ebCreateEnvironment(
      ebClient.value,
      (ebApplicationName in aws).value,
      (ebEnvironmentName in aws).value,
      (ebEnvironmentDescription in aws).value,
      (ebEnvironmentUseVersionLabel in aws).value,
      (ebEnvironmentTier in aws).value,
      (ebSolutionStackName in aws).value,
      (ebConfigurationTemplateName in aws).value,
      (ebConfigurationOptionSettings in aws).value,
      (ebOptionSpecifications in aws).value,
      (ebTags in aws).value,
      (ebCNAMEPrefix in aws).value
    ).get
  }

  def ebCreateEnvironmentAndWaitTask: Def.Initialize[Task[EnvironmentDescription]] = Def.task {
    implicit val logger = streams.value.log
    val result = (ebEnvironmentCreate in aws).value
    val (progressStatuses, headOption) = wait(ebClient.value) {
      ebDescribeEnvironment(ebClient.value, result.getEnvironmentId).get
    } {
      e =>
        e.exists { e =>
          val status = EnvironmentStatus.fromValue(e.statusOpt.get)
          status == EnvironmentStatus.Terminated || status == EnvironmentStatus.Ready
        }
    }
    progressStatuses.foreach { s =>
      logger.info(s"status = $s")
      TimeUnit.SECONDS.sleep(waitingIntervalInSec)
    }
    headOption().flatten.get
  }

  private[eb] def ebUpdateEnvironment(client: AWSElasticBeanstalkClient,
                                      applicationName: String,
                                      environmentName: String,
                                      description: Option[String],
                                      versionLabel: Option[String],
                                      tier: Option[EnvironmentTier],
                                      solutionStackName: Option[String],
                                      configurationTemplateName: Option[String],
                                      configurationOptionSettings: Seq[ConfigurationOptionSetting],
                                      optionSpecifications: Seq[OptionSpecification])(implicit logger: Logger): Try[EnvironmentDescription] = {
    logger.info(s"update environment start: $applicationName, $environmentName, $description, $versionLabel, $tier, $solutionStackName, $configurationTemplateName")
    ebDescribeEnvironment(client, applicationName, environmentName, versionLabel).flatMap { environment =>
      if (environment.isDefined) {
        val request = UpdateEnvironmentRequestFactory
          .create()
          .withEnvironmentIdOpt(environment.get.environmentIdOpt)
          .withEnvironmentName(environmentName)
          .withVersionLabelOpt(versionLabel)
          .withDescriptionOpt(description)
          .withTierOpt(tier)
          .withTemplateNameOpt(configurationTemplateName)
          .withOptionSettings(configurationOptionSettings: _*)
          .withOptionsToRemove(optionSpecifications: _*)
        val result = client.updateEnvironmentAsTry(request)
        logger.info(s"update environment finish: $applicationName, $environmentName, $description, $versionLabel, $tier, $solutionStackName, $configurationTemplateName")
        result.map { r =>
          EnvironmentDescriptionFactory
            .create()
            .withApplicationName(r.getApplicationName)
            .withCNAME(r.getCNAME)
            .withDateCreated(r.getDateCreated)
            .withDateUpdated(r.getDateUpdated)
            .withDescription(r.getDescription)
            .withEndpointURL(r.getEndpointURL)
            .withEnvironmentId(r.getEnvironmentId)
            .withEnvironmentName(r.getEnvironmentName)
            .withHealth(r.getHealth)
            .withResources(r.getResources)
            .withSolutionStackName(r.getSolutionStackName)
            .withStatus(r.getStatus)
            .withTemplateName(r.getTemplateName)
            .withTier(r.getTier)
            .withVersionLabel(r.getVersionLabel)
        }
      } else {
        logger.warn(s"not found environment: $applicationName, $environmentName")
        throw NotFoundException(s"The environment is not found: $applicationName, $environmentName")
      }
    }
  }

  def ebUpdateEnvironmentTask: Def.Initialize[Task[EnvironmentDescription]] = Def.task {
    implicit val logger = streams.value.log
    ebUpdateEnvironment(
      ebClient.value,
      (ebApplicationName in aws).value,
      (ebEnvironmentName in aws).value,
      (ebEnvironmentDescription in aws).value,
      (ebEnvironmentUseVersionLabel in aws).value,
      (ebEnvironmentTier in aws).value,
      (ebSolutionStackName in aws).value,
      (ebConfigurationTemplateName in aws).value,
      (ebConfigurationOptionSettings in aws).value,
      (ebOptionSpecifications in aws).value
    ).get
  }

  def ebUpdateEnvironmentAndWaitTask: Def.Initialize[Task[EnvironmentDescription]] = Def.task {
    implicit val logger = streams.value.log
    val result = (ebEnvironmentUpdate in aws).value
    val (progressStatuses, headOption) = wait(ebClient.value) {
      ebDescribeEnvironment(ebClient.value, result.getEnvironmentId).get
    } {
      _.exists { e =>
        val status = EnvironmentStatus.fromValue(e.statusOpt.get)
        status == EnvironmentStatus.Terminated || status == EnvironmentStatus.Ready
      }
    }
    progressStatuses.foreach { s =>
      logger.info(s"status = $s")
      TimeUnit.SECONDS.sleep(waitingIntervalInSec)
    }
    headOption().flatten.get
  }

  def ebCreateOrUpdateEnvironmentTask: Def.Initialize[Task[EnvironmentDescription]] = Def.task {
    implicit val logger = streams.value.log
    ebUpdateEnvironment(
      ebClient.value,
      (ebApplicationName in aws).value,
      (ebEnvironmentName in aws).value,
      (ebEnvironmentDescription in aws).value,
      (ebEnvironmentUseVersionLabel in aws).value,
      (ebEnvironmentTier in aws).value,
      (ebSolutionStackName in aws).value,
      (ebConfigurationTemplateName in aws).value,
      (ebConfigurationOptionSettings in aws).value,
      (ebOptionSpecifications in aws).value
    ).map { r =>
        EnvironmentDescriptionFactory
          .create()
          .withApplicationName(r.getApplicationName)
          .withCNAME(r.getCNAME)
          .withDateCreated(r.getDateCreated)
          .withDateUpdated(r.getDateUpdated)
          .withDescription(r.getDescription)
          .withEndpointURL(r.getEndpointURL)
          .withEnvironmentId(r.getEnvironmentId)
          .withEnvironmentName(r.getEnvironmentName)
          .withHealth(r.getHealth)
          .withResources(r.getResources)
          .withSolutionStackName(r.getSolutionStackName)
          .withStatus(r.getStatus)
          .withTemplateName(r.getTemplateName)
          .withTier(r.getTier)
          .withVersionLabel(r.getVersionLabel)
      }.recoverWith {
        case ex: NotFoundException =>
          ebCreateEnvironment(
            ebClient.value,
            (ebApplicationName in aws).value,
            (ebEnvironmentName in aws).value,
            (ebEnvironmentDescription in aws).value,
            (ebEnvironmentUseVersionLabel in aws).value,
            (ebEnvironmentTier in aws).value,
            (ebSolutionStackName in aws).value,
            (ebConfigurationTemplateName in aws).value,
            (ebConfigurationOptionSettings in aws).value,
            (ebOptionSpecifications in aws).value,
            (ebTags in aws).value,
            (ebCNAMEPrefix in aws).value
          ).map { r =>
              EnvironmentDescriptionFactory
                .create()
                .withApplicationName(r.getApplicationName)
                .withCNAME(r.getCNAME)
                .withDateCreated(r.getDateCreated)
                .withDateUpdated(r.getDateUpdated)
                .withDescription(r.getDescription)
                .withEndpointURL(r.getEndpointURL)
                .withEnvironmentId(r.getEnvironmentId)
                .withEnvironmentName(r.getEnvironmentName)
                .withHealth(r.getHealth)
                .withResources(r.getResources)
                .withSolutionStackName(r.getSolutionStackName)
                .withStatus(r.getStatus)
                .withTemplateName(r.getTemplateName)
                .withTier(r.getTier)
                .withVersionLabel(r.getVersionLabel)
            }
      }.get
  }

  def ebCreateOrUpdateEnvironmentAndWaitTask: Def.Initialize[Task[EnvironmentDescription]] = Def.task {
    implicit val logger = streams.value.log
    val result = (ebEnvironmentCreateOrUpdate in aws).value
    val (progressStatuses, headOption) = wait(ebClient.value) {
      ebDescribeEnvironment(ebClient.value, result.getEnvironmentId).get
    } {
      _.exists { e =>
        val status = EnvironmentStatus.fromValue(e.statusOpt.get)
        status == EnvironmentStatus.Terminated || status == EnvironmentStatus.Ready
      }
    }
    progressStatuses.foreach { s =>
      logger.info(s"status = $s")
      TimeUnit.SECONDS.sleep(waitingIntervalInSec)
    }
    headOption().flatten.get
  }

}

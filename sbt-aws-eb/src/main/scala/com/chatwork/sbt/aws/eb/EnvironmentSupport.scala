package com.chatwork.sbt.aws.eb

import java.util.concurrent.TimeUnit

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient
import com.amazonaws.services.elasticbeanstalk.model._
import com.chatwork.sbt.aws.core.SbtAwsCoreKeys._
import com.chatwork.sbt.aws.eb.SbtAwsEbKeys._
import org.sisioh.aws4s.eb.Implicits._
import org.sisioh.aws4s.eb.model._
import sbt.Keys._
import sbt._
import sbt.complete.DefaultParsers.{Space, StringBasic}
import sbt.complete.Parser._

import scala.util.Try

trait EnvironmentSupport { this: SbtAwsEb =>

  private[eb] val oneStringParser = token(
    opt(Space) ~> (opt("env=" ~> StringBasic) || opt("suffix=" ~> StringBasic)),
    "environemnt name")

  private[eb] def ebRestartAppServer(
      client: AWSElasticBeanstalkClient,
      applicationName: String,
      environmentName: String)(implicit logger: Logger): Try[EnvironmentDescription] = {
    ebDescribeEnvironment(client, applicationName, environmentName).flatMap {
      _.map { result =>
        val request = new RestartAppServerRequest().withEnvironmentId(result.getEnvironmentId)
        client.restartAppServerAsTry(request).map { _ =>
          result
        }
      }.get
    }
  }

  def ebRestartAppServerTask(): Def.Initialize[InputTask[EnvironmentDescription]] = Def.inputTask {
    val environmentNameEither = oneStringParser.parsed
    implicit val logger       = streams.value.log
    ebRestartAppServer(
      ebClient.value,
      (ebApplicationName in aws).value,
      environmentNameEither.fold(
        { envName =>
          envName.getOrElse((ebEnvironmentName in aws).value)
        }, { suffix =>
          suffix
            .map(e => (ebApplicationName in aws).value + "-" + e)
            .getOrElse((ebEnvironmentName in aws).value)
        }
      )
    ).get
  }

  private[eb] def ebDescribeEnvironment(
      client: AWSElasticBeanstalkClient,
      applicationName: String,
      environmentName: String)(implicit logger: Logger): Try[Option[EnvironmentDescription]] = {
    val request = DescribeEnvironmentsRequestFactory
      .create()
      .withApplicationName(applicationName)
      .withEnvironmentNames(environmentName)
    client.describeEnvironmentsAsTry(request).map {
      _.environments.find { e =>
        e.applicationNameOpt.get == applicationName &&
        e.environmentNameOpt.get == environmentName
      }
    }
  }

  private[eb] def ebDescribeEnvironment(client: AWSElasticBeanstalkClient, environmentId: String)(
      implicit logger: Logger): Try[Option[EnvironmentDescription]] = {
    val request = DescribeEnvironmentsRequestFactory
      .create()
      .withEnvironmentIds(environmentId)
    client.describeEnvironmentsAsTry(request).map {
      _.environments.find {
        _.environmentIdOpt.get == environmentId
      }
    }
  }

  private[eb] def ebCreateEnvironment(
      client: AWSElasticBeanstalkClient,
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
    ebDescribeEnvironment(client, applicationName, environmentName).flatMap {
      _.map { _ =>
        logger.warn(s"The application already exists.: $applicationName, $environmentName")
        throw AlreadyExistsException(
          s"The application already exists.: $applicationName, $environmentName")
      }.getOrElse {
        logger.info(
          s"create environment start: $applicationName, $environmentName, $description, $versionLabel, $tier, $solutionStackName, $configurationTemplateName")
        val baseRequest = CreateEnvironmentRequestFactory
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
        val request =
          if (tier.fold(false)(_.getType == "WebServer"))
            baseRequest.withCNAMEPrefixOpt(cnamePrefix)
          else baseRequest

        val result = client.createEnvironmentAsTry(request)
        logger.info(
          s"create environment finish: $applicationName, $environmentName, $description, $versionLabel, $tier, $solutionStackName, $configurationTemplateName")
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
      }
    }
  }

  def ebCreateEnvironmentTask(): Def.Initialize[InputTask[EnvironmentDescription]] =
    Def.inputTask {
      val environmentNameEither = oneStringParser.parsed
      implicit val logger       = streams.value.log
      ebCreateEnvironment(
        ebClient.value,
        (ebApplicationName in aws).value,
        environmentNameEither.fold(
          { envName =>
            envName.getOrElse((ebEnvironmentName in aws).value)
          }, { suffix =>
            suffix
              .map(e => (ebApplicationName in aws).value + "-" + e)
              .getOrElse((ebEnvironmentName in aws).value)
          }
        ),
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

  def ebCreateEnvironmentAndWaitTask(): Def.Initialize[InputTask[EnvironmentDescription]] =
    Def.inputTask {
      implicit val logger = streams.value.log
      val result          = (ebEnvironmentCreate in aws).evaluated
      val (progressStatuses, headOption) = wait(ebClient.value) {
        ebDescribeEnvironment(ebClient.value, result.getEnvironmentId).get
      } { e =>
        e.exists { e =>
          val status = EnvironmentStatus.fromValue(e.statusOpt.get)
          status == EnvironmentStatus.Terminated || status == EnvironmentStatus.Ready
        }
      }
      progressStatuses.foreach { s =>
        val status = s.map { e =>
          e.getApplicationName +
            "/" +
            e.getEnvironmentName +
            Option(e.getVersionLabel).map(e => "/" + e).getOrElse("") +
            "/" +
            e.getStatus
        }.get
        logger.info(s"$status : INPROGRESS")
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
                                      optionSpecifications: Seq[OptionSpecification])(
      implicit logger: Logger): Try[EnvironmentDescription] = {
    //If we specified a version to update to, the version has to exist in the application.
    versionLabel foreach { version =>
      val applicationVersion = describeApplicationVersion(client, applicationName, version)
      if (applicationVersion.isFailure || applicationVersion.get.isEmpty) {
        logger.warn(s"not found version: $applicationName, $environmentName, ${versionLabel.get}")
        throw ApplicationVersionNotFoundException(
          s"The specified application version is not found: $applicationName, $environmentName, ${versionLabel.get}")
      }
    }

    ebDescribeEnvironment(client, applicationName, environmentName).flatMap {
      _.map { environment =>
        if (EnvironmentStatus.valueOf(environment.getStatus) == EnvironmentStatus.Ready) {
          logger.info(
            s"update environment start: $applicationName, $environmentName, $description, $versionLabel, $tier, $solutionStackName, $configurationTemplateName, $configurationOptionSettings, $optionSpecifications")
          val request = UpdateEnvironmentRequestFactory
            .create()
            .withEnvironmentIdOpt(environment.environmentIdOpt)
            .withEnvironmentName(environmentName)
            .withVersionLabelOpt(versionLabel)
            .withDescriptionOpt(description)
            .withTierOpt(tier)
            .withTemplateNameOpt(configurationTemplateName)
            .withOptionSettings(configurationOptionSettings: _*)
            .withOptionsToRemove(optionSpecifications: _*)
          val result = client
            .updateEnvironmentAsTry(request)
            .map { r =>
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
            .recoverWith {
              case ex: AmazonServiceException if ex.getStatusCode == 404 =>
                logger.warn(s"not found environment: $applicationName, $environmentName")
                throw EnvironmentNotFoundException(
                  s"The environment is not found: $applicationName, $environmentName")
            }
          logger.info(
            s"update environment finish: $applicationName, $environmentName, $description, $versionLabel, $tier, $solutionStackName, $configurationTemplateName, $configurationOptionSettings, $optionSpecifications")
          result
        } else {
          logger.warn(s"not ready environment: $applicationName, $environmentName")
          throw EnvironmentNotReadyException(
            s"The environment is not ready: $applicationName, $environmentName")
        }
      }.getOrElse {
        logger.warn(s"not found environment: $applicationName, $environmentName")
        throw EnvironmentNotFoundException(
          s"The environment is not found: $applicationName, $environmentName")
      }
    }
  }

  def ebUpdateEnvironmentTask(): Def.Initialize[InputTask[EnvironmentDescription]] =
    Def.inputTask {
      val environmentNameEither = oneStringParser.parsed
      implicit val logger       = streams.value.log
      ebUpdateEnvironment(
        ebClient.value,
        (ebApplicationName in aws).value,
        environmentNameEither.fold(
          { envName =>
            envName.getOrElse((ebEnvironmentName in aws).value)
          }, { suffix =>
            suffix
              .map(e => (ebApplicationName in aws).value + "-" + e)
              .getOrElse((ebEnvironmentName in aws).value)
          }
        ),
        (ebEnvironmentDescription in aws).value,
        (ebEnvironmentUseVersionLabel in aws).value,
        (ebEnvironmentTier in aws).value,
        (ebSolutionStackName in aws).value,
        (ebConfigurationTemplateName in aws).value,
        (ebConfigurationOptionSettings in aws).value,
        (ebOptionSpecifications in aws).value
      ).get
    }

  def ebUpdateEnvironmentAndWaitTask(): Def.Initialize[InputTask[EnvironmentDescription]] =
    Def.inputTask {
      implicit val logger = streams.value.log
      val result          = (ebEnvironmentUpdate in aws).evaluated
      val (progressStatuses, headOption) = wait(ebClient.value) {
        ebDescribeEnvironment(ebClient.value, result.getEnvironmentId).get
      } {
        _.exists { e =>
          val status = EnvironmentStatus.fromValue(e.statusOpt.get)
          status == EnvironmentStatus.Terminated || status == EnvironmentStatus.Ready
        }
      }
      progressStatuses.foreach { s =>
        val status = s.map { e =>
          e.getApplicationName +
            "/" +
            e.getEnvironmentName +
            Option(e.getVersionLabel).map(e => "/" + e).getOrElse("") +
            "/" +
            e.getStatus
        }.get
        logger.info(s"$status : INPROGRESS")
        TimeUnit.SECONDS.sleep(waitingIntervalInSec)
      }
      headOption().flatten.get
    }

  def ebCreateOrUpdateEnvironmentTask(): Def.Initialize[InputTask[EnvironmentDescription]] =
    Def.inputTask {
      val environmentNameEither = oneStringParser.parsed
      implicit val logger       = streams.value.log
      ebUpdateEnvironment(
        ebClient.value,
        (ebApplicationName in aws).value,
        environmentNameEither.fold(
          { envName =>
            envName.getOrElse((ebEnvironmentName in aws).value)
          }, { suffix =>
            suffix
              .map(e => (ebApplicationName in aws).value + "-" + e)
              .getOrElse((ebEnvironmentName in aws).value)
          }
        ),
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
        case ex: EnvironmentNotFoundException =>
          ebCreateEnvironment(
            ebClient.value,
            (ebApplicationName in aws).value,
            environmentNameEither.fold(
              { envName =>
                envName.getOrElse((ebEnvironmentName in aws).value)
              }, { suffix =>
                suffix
                  .map(e => (ebApplicationName in aws).value + "-" + e)
                  .getOrElse((ebEnvironmentName in aws).value)
              }
            ),
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

  def ebCreateOrUpdateEnvironmentAndWaitTask(): Def.Initialize[InputTask[EnvironmentDescription]] =
    Def.inputTask {
      implicit val logger = streams.value.log
      val result          = (ebEnvironmentCreateOrUpdate in aws).evaluated
      val (progressStatuses, headOption) = wait(ebClient.value) {
        ebDescribeEnvironment(ebClient.value, result.getEnvironmentId).get
      } {
        _.exists { e =>
          val status = EnvironmentStatus.fromValue(e.statusOpt.get)
          status == EnvironmentStatus.Terminated || status == EnvironmentStatus.Ready
        }
      }
      progressStatuses.foreach { s =>
        val status = s.map { e =>
          e.getApplicationName +
            "/" +
            e.getEnvironmentName +
            Option(e.getVersionLabel).map(e => "/" + e).getOrElse("") +
            "/" +
            e.getStatus
        }.get
        logger.info(s"$status : INPROGRESS")
        TimeUnit.SECONDS.sleep(waitingIntervalInSec)
      }
      headOption().flatten.get
    }

}

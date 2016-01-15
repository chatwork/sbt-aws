package com.chatwork.sbt.aws.eb

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient
import com.amazonaws.services.elasticbeanstalk.model.UpdateConfigurationTemplateResult
import com.chatwork.sbt.aws.core.SbtAwsCoreKeys._
import com.chatwork.sbt.aws.eb.SbtAwsEbPlugin.autoImport
import org.sisioh.aws4s.eb.Implicits._
import org.sisioh.aws4s.eb.model._
import sbt.Keys._
import sbt._

import scala.collection.JavaConverters._
import scala.util.Try

trait ConfigurationTemplateSupport {
  this: SbtAwsEb =>

  import autoImport._

  private[eb] def ebCreateConfigurationTemplate(client: AWSElasticBeanstalkClient,
                                                applicationName: String,
                                                ebConfigurationTemplate: EbConfigurationTemplate)(implicit logger: Logger): Try[EbConfigurationTemplateDescription] = {
    logger.info(s"create configuration template start: $applicationName, $ebConfigurationTemplate")
    val request = CreateConfigurationTemplateRequestFactory
      .create()
      .withTemplateName(ebConfigurationTemplate.name)
      .withApplicationName(applicationName)
      .withDescriptionOpt(ebConfigurationTemplate.description)
      .withSolutionStackName(ebConfigurationTemplate.solutionStackName)
      .withOptionSettings(ebConfigurationTemplate.optionSettings)
    val result = client.createConfigurationTemplateAsTry(request).map { e =>
      EbConfigurationTemplateDescription(
        e.getTemplateName,
        Option(e.getDescription),
        e.getDeploymentStatus,
        e.getApplicationName,
        e.getEnvironmentName,
        e.getSolutionStackName,
        e.getOptionSettings.asScala.map { v =>
          EbConfigurationOptionSetting(v.getNamespace, v.getOptionName, v.getValue)
        },
        e.getDateCreated,
        e.getDateUpdated
      )
    }
    logger.info(s"create configuration template finish: $applicationName, $ebConfigurationTemplate")
    result
  }

  def ebCreateConfigurationTemplateTask(): Def.Initialize[Task[EbConfigurationTemplateDescription]] = Def.task {
    implicit val logger = streams.value.log
    ebCreateConfigurationTemplate(
      ebClient.value,
      (ebApplicationName in aws).value,
      (ebConfigurationTemplate in aws).value.get
    ).get
  }

  private val pattern = "No Configuration Template named"

  private[eb] def ebUpdateConfigurationTemplate(client: AWSElasticBeanstalkClient,
                                                applicationName: String,
                                                ebConfigurationTemplate: EbConfigurationTemplate)(implicit logger: Logger): Try[EbConfigurationTemplateDescription] = {
    logger.info(s"update configuration template start: $applicationName, $ebConfigurationTemplate")
    val request = UpdateConfigurationTemplateRequestFactory
      .create()
      .withTemplateName(ebConfigurationTemplate.name)
      .withApplicationName(applicationName)
      .withDescriptionOpt(ebConfigurationTemplate.description)
      .withOptionSettings(ebConfigurationTemplate.optionSettings)
      .withOptionsToRemove(ebConfigurationTemplate.optionsToRemoves)
    val result = client.updateConfigurationTemplateAsTry(request).map { result =>
      logger.info(s"update configuration template finish: $applicationName, $ebConfigurationTemplate")
      result
    }.map { e =>
      EbConfigurationTemplateDescription(
        e.getTemplateName,
        Option(e.getDescription),
        e.getDeploymentStatus,
        e.getApplicationName,
        e.getEnvironmentName,
        e.getSolutionStackName,
        e.getOptionSettings.asScala.map { v =>
          EbConfigurationOptionSetting(v.getNamespace, v.getOptionName, v.getValue)
        },
        e.getDateCreated,
        e.getDateUpdated
      )
    }.recoverWith {
      case ex: AmazonServiceException if ex.getStatusCode == 400 && ex.getMessage.startsWith(pattern) =>
        logger.warn(s"The configuration template is not found.: $applicationName, ${ebConfigurationTemplate.name}")
        throw ConfigurationTemplateNotFoundException(s"The configuration template is not found.: $applicationName, ${ebConfigurationTemplate.name}", Some(ex))
      case ex: AmazonServiceException if ex.getStatusCode == 404 =>
        logger.warn(s"The configuration template is not found.: $applicationName, ${ebConfigurationTemplate.name}")
        throw ConfigurationTemplateNotFoundException(s"The configuration template is not found.: $applicationName, ${ebConfigurationTemplate.name}", Some(ex))
    }
    result
  }

  def ebUpdateConfigurationTemplateTask(): Def.Initialize[Task[EbConfigurationTemplateDescription]] = Def.task {
    implicit val logger = streams.value.log
    ebUpdateConfigurationTemplate(
      ebClient.value,
      (ebApplicationName in aws).value,
      (ebConfigurationTemplate in aws).value.get
    ).get
  }

  def ebCreateOrUpdateConfigurationTemplateTask(): Def.Initialize[Task[EbConfigurationTemplateDescription]] = Def.task {
    implicit val logger = streams.value.log
    ebUpdateConfigurationTemplate(
      ebClient.value,
      (ebApplicationName in aws).value,
      (ebConfigurationTemplate in aws).value.get
    ).recoverWith {
        case ex: ConfigurationTemplateNotFoundException =>
          ebCreateConfigurationTemplate(
            ebClient.value,
            (ebApplicationName in aws).value,
            (ebConfigurationTemplate in aws).value.get
          )
      }.get
  }

  private[eb] def ebDeleteConfigurationTemplate(client: AWSElasticBeanstalkClient,
                                                applicationName: String,
                                                ebConfigurationTemplate: EbConfigurationTemplate)(implicit logger: Logger): Try[Unit] = {
    logger.info(s"delete configuration template start: $applicationName, $ebConfigurationTemplate")
    val request = DeleteConfigurationTemplateRequestFactory
      .create()
      .withApplicationName(applicationName)
      .withTemplateName(ebConfigurationTemplate.name)
    val result = client.deleteConfigurationTemplateAsTry(request).map { _ =>
      logger.info(s"delete configuration template finish: $applicationName, $ebConfigurationTemplate")
    }.recoverWith {
      case ex: AmazonServiceException if ex.getStatusCode == 404 =>
        logger.warn(s"The configuration template is not found.: $applicationName, ${ebConfigurationTemplate.name}")
        throw ConfigurationTemplateNotFoundException(s"The configuration template is not found.: $applicationName, ${ebConfigurationTemplate.name}", Some(ex))
    }
    result
  }

  def ebDeleteConfigurationTemplateTask(): Def.Initialize[Task[Unit]] = Def.task {
    implicit val logger = streams.value.log
    ebDeleteConfigurationTemplate(
      ebClient.value,
      (ebApplicationName in aws).value,
      (ebConfigurationTemplate in aws).value.get
    ).recover {
        case ex: ConfigurationTemplateNotFoundException =>
          ()
      }.get
  }

}

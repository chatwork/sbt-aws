package com.chatwork.sbt.aws.eb

import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient
import com.amazonaws.services.elasticbeanstalk.model.{CreateConfigurationTemplateResult, UpdateConfigurationTemplateResult}
import com.chatwork.sbt.aws.core.SbtAwsCoreKeys._
import com.chatwork.sbt.aws.eb.SbtAwsEbKeys._
import com.chatwork.sbt.aws.eb.model.EbConfigurationTemplate
import org.sisioh.aws4s.eb.Implicits._
import org.sisioh.aws4s.eb.model._
import sbt.Keys._
import sbt._

import scala.util.Try

trait ConfigurationTemplateSupport {
  this: SbtAwsEb =>

  private[eb] def ebCreateConfigurationTemplate(client: AWSElasticBeanstalkClient,
                                                applicationName: String,
                                                ebConfigurationTemplate: EbConfigurationTemplate)(implicit logger: Logger): Try[CreateConfigurationTemplateResult] = {
    logger.info(s"create configuration template start: $applicationName, $ebConfigurationTemplate")
    val request = CreateConfigurationTemplateRequestFactory
      .create()
      .withApplicationName(applicationName)
      .withTemplateName(ebConfigurationTemplate.name)
      .withDescription(ebConfigurationTemplate.description)
      .withOptionSettings(ebConfigurationTemplate.optionSettings)
    val result = client.createConfigurationTemplateAsTry(request)
    logger.info(s"create configuration template finish: $applicationName, $ebConfigurationTemplate")
    result
  }

  def ebCreateConfigurationTemplateTask(): Def.Initialize[Task[CreateConfigurationTemplateResult]] = Def.task {
    implicit val logger = streams.value.log
    ebCreateConfigurationTemplate(
      ebClient.value,
      (ebApplicationName in aws).value,
      (ebConfigurationTemplate in aws).value.get
    ).get
  }

  private[eb] def ebUpdateConfigurationTemplate(client: AWSElasticBeanstalkClient,
                                                applicationName: String,
                                                ebConfigurationTemplate: EbConfigurationTemplate)(implicit logger: Logger): Try[UpdateConfigurationTemplateResult] = {
    logger.info(s"update configuration template start: $applicationName, $ebConfigurationTemplate")
    val request = UpdateConfigurationTemplateRequestFactory
      .create()
      .withApplicationName(applicationName)
      .withTemplateName(ebConfigurationTemplate.name)
      .withDescription(ebConfigurationTemplate.description)
      .withOptionSettings(ebConfigurationTemplate.optionSettings)
      .withOptionsToRemove(ebConfigurationTemplate.optionsToRemoves)
    val result = client.updateConfigurationTemplateAsTry(request)
    logger.info(s"update configuration template finish: $applicationName, $ebConfigurationTemplate")
    result
  }

  private[eb] def ebUpdateConfigurationTemplateTask(): Def.Initialize[Task[UpdateConfigurationTemplateResult]] = Def.task {
    implicit val logger = streams.value.log
    ebUpdateConfigurationTemplate(
      ebClient.value,
      (ebApplicationName in aws).value,
      (ebConfigurationTemplate in aws).value.get
    ).get
  }

  private[eb] def ebDeleteConfigurationTemplate(client: AWSElasticBeanstalkClient,
                                                applicationName: String,
                                                ebConfigurationTemplate: EbConfigurationTemplate)(implicit logger: Logger): Try[Unit] = {
    logger.info(s"delete configuration template start: $applicationName, $ebConfigurationTemplate")
    val request = DeleteConfigurationTemplateRequestFactory
      .create()
      .withApplicationName(applicationName)
      .withTemplateName(ebConfigurationTemplate.name)
    val result = client.deleteConfigurationTemplateAsTry(request)
    logger.info(s"delete configuration template finish: $applicationName, $ebConfigurationTemplate")
    result
  }

  def ebDeleteConfigurationTemplateTask(): Def.Initialize[Task[Unit]] = Def.task {
    implicit val logger = streams.value.log
    ebDeleteConfigurationTemplate(
      ebClient.value,
      (ebApplicationName in aws).value,
      (ebConfigurationTemplate in aws).value.get
    ).get
  }

}

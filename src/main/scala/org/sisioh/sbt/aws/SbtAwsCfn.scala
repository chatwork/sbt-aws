package org.sisioh.sbt.aws

import java.io.FileNotFoundException
import java.util

import com.amazonaws.AmazonServiceException
import com.amazonaws.regions.Region
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient
import com.amazonaws.services.cloudformation.model.{ Parameter, Stack, Tag }
import org.sisioh.aws4s.cfn.Implicits._
import org.sisioh.aws4s.cfn.model._
import org.sisioh.sbt.aws.AwsKeys.CfnKeys._
import org.sisioh.sbt.aws.AwsKeys._
import sbt.Keys._
import sbt._

import scala.collection.JavaConverters._
import scala.util.{ Failure, Success, Try }

trait SbtAwsCfn {
  this: SbtAws.type =>

  lazy val cfnClient = Def.task {
    createClient(classOf[AmazonCloudFormationClient], Region.getRegion((region in aws).value), (credentialProfileName in aws).value)
  }

  def stackTemplatesTask(): Def.Initialize[Task[String]] = Def.task {
    val files = (cfnTemplates in aws).value
    IO.read(files.headOption.getOrElse(throw new FileNotFoundException("*.template not found in this project")))
  }

  def validateTemplate(client: AmazonCloudFormationClient, log: Logger)(template: sbt.File): (File, Try[Seq[String]]) = {
    (template, {
      val request = ValidateTemplateRequestFactory.create().withTemplateBody(IO.read(template))
      client.validateTemplateAsTry(request).map {
        result =>
          log.debug(s"result from validating $template : $result")
          log.info(s"validated $template")
          result.parameters.map(_.parameterKeyOpt.get)
      }
    })
  }

  def stackValidateTask(): Def.Initialize[Task[Seq[sbt.File]]] = Def.task {
    val logger = streams.value.log
    val files = (cfnTemplates in aws).value
    logger.info("stack validate: files = " + files)
    val results = files.map(validateTemplate(cfnClient.value, logger))
    results.foreach { tr =>
      tr._2 match {
        case Failure(e) => logger.error(s"validation of ${tr._1} failed with: \n ${e.getMessage}")
        case _          =>
      }
    }
    if (results.exists(_._2.isFailure)) {
      sys.error("some AWS CloudFormation templates failed to validate!")
    }
    files
  }

  def describeStacksTask(): Def.Initialize[Task[Seq[Stack]]] = Def.task {
    val logger = streams.value.log
    val stackName = (cfnStackName in aws).value
    logger.info("stack name: name = " + stackName)
    val request = DescribeStacksRequestFactory.create().withStackName(stackName)
    val result = cfnClient.value.describeStacksAsTry(request).map { result =>
      result.stacks
    }.recoverWith {
      case ex: AmazonServiceException if ex.getStatusCode == 400 =>
        Success(Seq.empty)
    }
    result.foreach { stacks =>
      stacks.foreach(stack => logger.info(stack.toString))
    }
    result.getOrElse(Seq.empty)
  }

  def getStackStatus(client: AmazonCloudFormationClient, stackName: String): Try[Option[String]] = {
    val request = DescribeStacksRequestFactory.create().withStackName(stackName)
    client.describeStacksAsTry(request).map { result =>
      result.stacks.headOption.flatMap { stack => stack.stackStatusOpt }
    }
  }

  def statusStackTask(): Def.Initialize[Task[Option[String]]] = Def.task {
    val logger = streams.value.log
    val stackName = (cfnStackName in aws).value
    logger.info("stack name: name = " + stackName)
    val result = getStackStatus(cfnClient.value, stackName)
    result.foreach { statusOpt =>
      statusOpt.foreach { status =>
        logger.info(status)
      }
    }
    result.get
  }

  implicit private def parametersToList(params: Parameters): util.Collection[Parameter] = {
    val ps: Iterable[Parameter] = for {
      (k, v) <- params
    } yield {
      val p = new Parameter()
      p.setParameterKey(k)
      p.setParameterValue(v)
      p
    }
    ps.toList.asJava
  }

  implicit private def tagsToList(tags: Tags): util.Collection[Tag] = {
    val ps: Iterable[Tag] = for {
      (k, v) <- tags
    } yield {
      val p = new Tag()
      p.setKey(k)
      p.setValue(v)
      p
    }
    ps.toList.asJava
  }

  def createStackTask(): Def.Initialize[Task[String]] = Def.task {
    val logger = streams.value.log
    val stackName = (cfnStackName in aws).value
    val templateBody = (cfnStackTemplate in aws).value
    val capabilities = (cfnStackCapabilities in aws).value
    val params = (cfnStackParams in aws).value
    val tags = (cfnStackTags in aws).value
    val client = cfnClient.value
    logger.info("stack name: name = " + stackName)
    val request = CreateStackRequestFactory
      .create()
      .withStackName(stackName)
      .withTemplateBody(templateBody)
      .withCapabilities(capabilities)
      .withParameters(params)
      .withTags(tags)
    val result = client.createStackAsTry(request)
    result.foreach { result =>
      logger.info(s"created stack ${request.stackNameOpt.get} / ${result.stackIdOpt.get}")
    }
    result.map(_.stackIdOpt.get).get
  }

  def deleteStackTask(): Def.Initialize[Task[Unit]] = Def.task {
    val logger = streams.value.log
    val stackName = (cfnStackName in aws).value
    val client = cfnClient.value
    logger.info("stack name: name = " + stackName)
    val request = DeleteStackRequestFactory.create().withStackName(stackName)
    val result = client.deleteStackAsTry(request)
    result.foreach { result =>
      logger.info(s"deleted stack ${request.stackNameOpt.get}")
    }
  }

  def updateStackTask(): Def.Initialize[Task[String]] = Def.task {
    val logger = streams.value.log
    val stackName = (cfnStackName in aws).value
    val templateBody = (cfnStackTemplate in aws).value
    val capabilities = (cfnStackCapabilities in aws).value
    val params = (cfnStackParams in aws).value
    val client = cfnClient.value
    logger.info("stack name: name = " + stackName)
    val request = UpdateStackRequestFactory
      .create()
      .withStackName(stackName)
      .withTemplateBody(templateBody)
      .withCapabilities(capabilities)
      .withParameters(params)
    val result = client.updateStackAsTry(request)
    result.foreach { result =>
      logger.info(s"created stack ${request.stackNameOpt.get} / ${result.stackIdOpt.get}")
    }
    result.map(_.stackIdOpt.get).get
  }

  def waitStackTask(): Def.Initialize[Task[Option[String]]] = Def.task {
    val client = cfnClient.value
    val stackName = (cfnStackName in aws).value

    def statuses: Stream[String] = Stream.cons(getStackStatus(client, stackName).get.get, statuses)

    val progressStatuses: Stream[String] = statuses.takeWhile { status => status.endsWith("_PROGRESS") }
    progressStatuses foreach {
      s =>
        Thread.sleep(10000)
    }
    statuses.headOption
  }

}
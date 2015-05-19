package org.sisioh.sbt.aws

import java.io.FileNotFoundException
import java.util

import com.amazonaws.AmazonServiceException
import com.amazonaws.regions.Region
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient
import com.amazonaws.services.cloudformation.model._
import org.sisioh.aws4s.cfn.Implicits._
import org.sisioh.aws4s.cfn.model._
import org.sisioh.sbt.aws.SbtAwsPlugin.AwsKeys._
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

  def describeStacks(client: AmazonCloudFormationClient, stackName: String) = {
    val request = DescribeStacksRequestFactory.create().withStackName(stackName)
    client.describeStacksAsTry(request).map { result =>
      result.stacks
    }.recoverWith {
      case ex: AmazonServiceException if ex.getStatusCode == 400 =>
        Success(Seq.empty)
    }
  }

  def describeStacksTask(): Def.Initialize[Task[Seq[Stack]]] = Def.task {
    val logger = streams.value.log
    val stackName = (cfnStackName in aws).value
    val client = cfnClient.value

    logger.info(s"stackName = $stackName")

    val result = describeStacks(client, stackName)
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
    val client = cfnClient.value

    logger.info(s"stackName = $stackName")

    val result = getStackStatus(client, stackName).recoverWith {
      case ex: AmazonServiceException if ex.getStatusCode == 400 =>
        Success(None)
    }
    result.foreach {
      case Some(status) => logger.info(s"$stackName's status is $status")
      case None         => logger.info(s"$stackName does not exists.")
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

  def createStack(client: AmazonCloudFormationClient,
                  stackName: String, templateBody: String,
                  capabilities: Seq[String],
                  parameters: Map[String, String],
                  tags: Map[String, String]) = {
    val request = CreateStackRequestFactory
      .create()
      .withStackName(stackName)
      .withTemplateBody(templateBody)
      .withCapabilities(capabilities)
      .withParameters(parameters)
      .withTags(tags)
    client.createStackAsTry(request)
  }

  def createStackTask(): Def.Initialize[Task[Option[String]]] = Def.task {
    val logger = streams.value.log
    val stackName = (cfnStackName in aws).value
    val templateBody = (cfnStackTemplate in aws).value
    val capabilities = (cfnStackCapabilities in aws).value
    val params = (cfnStackParams in aws).value
    val tags = (cfnStackTags in aws).value
    val client = cfnClient.value

    logger.info(s"stackName = $stackName")
    logger.info(s"templateBody = $templateBody")
    logger.info(s"capabilities = $capabilities")
    logger.info(s"stackParams = $params")
    logger.info(s"tags = $tags")

    describeStacks(client, stackName).flatMap { stacks =>
      stacks.headOption.map { stack =>
        createStack(client, stackName, templateBody, capabilities, params, tags).map(Some(_))
      }.getOrElse {
        Success(None)
      }
    }.map { resultOpt =>
      resultOpt.map { result =>
        logger.info(s"created stack $stackName / ${result.stackIdOpt.get}")
        Some(result.stackIdOpt.get)
      }.getOrElse {
        logger.info(s"does not exists $stackName")
        None
      }
    }.get
  }

  def createStackAndWaitTask(): Def.Initialize[Task[Option[String]]] = Def.task {
    val logger = streams.value.log
    val stackName = (cfnStackName in aws).value
    val templateBody = (cfnStackTemplate in aws).value
    val capabilities = (cfnStackCapabilities in aws).value
    val params = (cfnStackParams in aws).value
    val tags = (cfnStackTags in aws).value
    val client = cfnClient.value
    val interval = (poolingInterval in aws).value

    logger.info(s"stackName = $stackName")
    logger.info(s"templateBody = $templateBody")
    logger.info(s"capabilities = $capabilities")
    logger.info(s"stackParams = $params")
    logger.info(s"tags = $tags")

    describeStacks(client, stackName).flatMap { stacks =>
      stacks.headOption.map { stack =>
        createStack(client, stackName, templateBody, capabilities, params, tags).map(Some(_))
      }.getOrElse {
        Success(None)
      }
    }.map { resultOpt =>
      resultOpt.map { result =>
        logger.info(s"created stack $stackName / ${result.stackIdOpt.get}")
        val (progressStatuses, headOption) = waitStack(client, stackName)
        progressStatuses.foreach {
          s =>
            logger.info(s"status = $s")
            Thread.sleep(interval)
        }
        headOption()
      }.getOrElse {
        logger.info(s"does not exists $stackName")
        None
      }
    }.get
  }

  def deleteStack(client: AmazonCloudFormationClient,
                  stackName: String) = {
    val request = DeleteStackRequestFactory.create().withStackName(stackName)
    client.deleteStackAsTry(request)
  }

  def deleteStackTask(): Def.Initialize[Task[Unit]] = Def.task {
    val logger = streams.value.log
    val stackName = (cfnStackName in aws).value
    val client = cfnClient.value

    logger.info(s"stackName = $stackName")

    describeStacks(client, stackName).flatMap { stacks =>
      stacks.headOption.map { stack =>
        deleteStack(client, stackName).map(Some(_))
      }.getOrElse {
        Success(None)
      }
    }.foreach { result =>
      if (result.isDefined)
        logger.info(s"deleted stack $stackName")
      else {
        logger.info(s"does not exists $stackName")
      }
    }
  }

  def deleteStackAndWaitTask(): Def.Initialize[Task[Option[String]]] = Def.task {
    val logger = streams.value.log
    val stackName = (cfnStackName in aws).value
    val client = cfnClient.value
    val interval = (poolingInterval in aws).value

    logger.info(s"stackName = $stackName")

    describeStacks(client, stackName).flatMap { stacks =>
      stacks.headOption.map { stack =>
        deleteStack(client, stackName).map(Some(_))
      }.getOrElse {
        Success(None)
      }
    }.map { resultOpt =>
      resultOpt.map { result =>
        logger.info(s"deleted stack $stackName")
        val (progressStatuses, headOption) = waitStack(client, stackName)
        progressStatuses.foreach {
          s =>
            logger.info(s"status = $s")
            Thread.sleep(interval)
        }
        headOption()
      }.getOrElse {
        logger.info(s"does not exists $stackName")
        None
      }
    }.get
  }

  def updateStack(client: AmazonCloudFormationClient,
                  stackName: String, templateBody: String,
                  capabilities: Seq[String],
                  parameters: Map[String, String]): Try[Option[UpdateStackResult]] = {
    val request = UpdateStackRequestFactory
      .create()
      .withStackName(stackName)
      .withTemplateBody(templateBody)
      .withCapabilities(capabilities)
      .withParameters(parameters)
    client.updateStackAsTry(request).map(e => Some(e)).recoverWith {
      case ex: AmazonServiceException if ex.getStatusCode == 400 =>
        Success(None)
    }
  }

  def updateStackTask(): Def.Initialize[Task[Option[String]]] = Def.task {
    val logger = streams.value.log
    val stackName = (cfnStackName in aws).value
    val templateBody = (cfnStackTemplate in aws).value
    val capabilities = (cfnStackCapabilities in aws).value
    val params = (cfnStackParams in aws).value
    val client = cfnClient.value

    logger.info(s"stackName = $stackName")
    logger.info(s"templateBody = $templateBody")
    logger.info(s"capabilities = $capabilities")
    logger.info(s"stackParams = $params")

    describeStacks(client, stackName).flatMap { stacks =>
      stacks.headOption.map { stack =>
        updateStack(client, stackName, templateBody, capabilities, params)
      }.getOrElse {
        Success(None)
      }
    }.map { resultOpt =>
      resultOpt.map { result =>
        logger.info(s"updated stack $stackName / ${result.stackIdOpt.get}")
        Some(result.stackIdOpt.get)
      }.getOrElse {
        logger.info("No updates are to be performed.")
        None
      }
    }.get
  }

  def updateStackAndWaitTask(): Def.Initialize[Task[Option[String]]] = Def.task {
    val logger = streams.value.log
    val stackName = (cfnStackName in aws).value
    val templateBody = (cfnStackTemplate in aws).value
    val capabilities = (cfnStackCapabilities in aws).value
    val params = (cfnStackParams in aws).value
    val client = cfnClient.value
    val interval = (poolingInterval in aws).value

    logger.info(s"stackName = $stackName")
    logger.info(s"templateBody = $templateBody")
    logger.info(s"capabilities = $capabilities")
    logger.info(s"stackParams = $params")

    describeStacks(client, stackName).flatMap { stacks =>
      stacks.headOption.map { stack =>
        updateStack(client, stackName, templateBody, capabilities, params)
      }.getOrElse {
        Success(None)
      }
    }.map { resultOpt =>
      resultOpt.map { result =>
        logger.info(s"updated stack $stackName / ${result.stackIdOpt.get}")
        val (progressStatuses, headOption) = waitStack(client, stackName)
        progressStatuses.foreach {
          s =>
            logger.info(s"status = $s")
            Thread.sleep(interval)
        }
        headOption()
      }.getOrElse {
        logger.info("No updates are to be performed.")
        None
      }
    }.get
  }

  def createOrUpdateStackTask(): Def.Initialize[Task[Option[String]]] = Def.task {
    val logger = streams.value.log
    val stackName = (cfnStackName in aws).value
    val templateBody = (cfnStackTemplate in aws).value
    val capabilities = (cfnStackCapabilities in aws).value
    val params = (cfnStackParams in aws).value
    val client = cfnClient.value
    val interval = (poolingInterval in aws).value
    val tags = (cfnStackTags in aws).value

    logger.info(s"stackName = $stackName")
    logger.info(s"templateBody = $templateBody")
    logger.info(s"capabilities = $capabilities")
    logger.info(s"stackParams = $params")
    logger.info(s"tags = $tags")

    describeStacks(client, stackName).flatMap { stacks =>
      stacks.headOption.map { stack =>
        updateStack(client, stackName, templateBody, capabilities, params)
      }.getOrElse {
        createStack(client, stackName, templateBody, capabilities, params, tags)
      }
    }.map {
      case result: CreateStackResult =>
        logger.info(s"created stack $stackName / ${result.stackIdOpt.get}")
        true
      case resultOpt: Option[_] =>
        resultOpt.map {
          case result: UpdateStackResult =>
            logger.info(s"updated stack $stackName / ${result.stackIdOpt.get}")
            true
        }.getOrElse {
          logger.info("No updates are to be performed.")
          false
        }
    }.map {
      case true =>
        val (progressStatuses, headOption) = waitStack(client, stackName)
        progressStatuses.foreach {
          s =>
            logger.info(s"status = $s")
            Thread.sleep(interval)
        }
        headOption()
      case false => None
    }.getOrElse(None)
  }

  def waitStack(client: AmazonCloudFormationClient,
                stackName: String) = {
    def statuses: Stream[String] = Stream.cons(getStackStatus(client, stackName).get.get, statuses)

    val progressStatuses: Stream[String] = statuses.takeWhile { status => status.endsWith("_PROGRESS") }
    (progressStatuses, () => statuses.headOption)
  }

  def waitStackTask(): Def.Initialize[Task[Option[String]]] = Def.task {
    val client = cfnClient.value
    val stackName = (cfnStackName in aws).value
    val interval = (poolingInterval in aws).value

    val (progressStatuses, headOption) = waitStack(client, stackName)
    progressStatuses.foreach {
      s =>
        Thread.sleep(interval)
    }
    headOption()

  }

}
package com.chatwork.sbt.aws.cfn

import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.util
import java.util.Date

import com.amazonaws.AmazonServiceException
import com.amazonaws.regions.Region
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient
import com.amazonaws.services.cloudformation.model._
import com.chatwork.sbt.aws.cfn.SbtAwsCfnKeys._
import com.chatwork.sbt.aws.core.SbtAwsCoreKeys._
import com.chatwork.sbt.aws.s3.SbtAwsS3
import org.sisioh.aws4s.cfn.Implicits._
import org.sisioh.aws4s.cfn.model._
import sbt.Keys._
import sbt._

import scala.collection.JavaConverters._
import scala.util.{ Failure, Success, Try }

object SbtAwsCfn extends SbtAwsCfn

trait SbtAwsCfn extends SbtAwsS3 {

  val defaultTemplateDirectory = "aws/cfn/templates"

  lazy val cfnClient = Def.task {
    val logger = streams.value.log
    val r = (region in aws).value
    val cpc = (credentialsProviderChain in aws).value
    logger.info(s"region = $r")
    createClient(
      cpc,
      classOf[AmazonCloudFormationClient],
      Region.getRegion(r)
    )
  }

  def uploadTemplateFileTask(): Def.Initialize[Task[URL]] = Def.task {
    val logger = streams.value.log
    val files = (cfnTemplates in aws).value
    val file = files.headOption.getOrElse {
      throw new FileNotFoundException("*.template not found in this project")
    }
    val bucketName = (cfnS3BucketName in aws).value

    logger.info(s"targetFile = $file, bucketName = $bucketName")

    require(bucketName.isDefined)

    logger.debug(IO.read(file))

    val sdf = new SimpleDateFormat("yyyyMMdd'_'HHmmss")
    val timestamp = sdf.format(new Date())

    val artifactId = (cfnArtifactId in aws).value
    val version = (cfnVersion in aws).value

    val keyMapper = (cfnS3KeyMapper in aws).value
    val key = keyMapper(s"$artifactId-$version-$timestamp.templete")

    logger.info(s"upload $file to ${bucketName.get}/$key")
    val result = s3PutObject(s3Client.value, bucketName.get, key, file, false, true)
    logger.info(s"uploaded $file to ${bucketName.get}/$key")
    new URL(result.get)
  }

  def validateTemplateOnURL(client: AmazonCloudFormationClient)(template: URL)(implicit logger: Logger): (URL, Try[Seq[String]]) = {
    (template, {
      val request = ValidateTemplateRequestFactory.create().withTemplateURL(template.toString)
      client.validateTemplateAsTry(request).map {
        result =>
          logger.debug(s"result from validating $template : $result")
          logger.info(s"validated $template, $result")
          result.parameters.map(_.parameterKeyOpt.get)
      }
    })
  }

  private[cfn] def stackValidateOnURLTask(): Def.Initialize[Task[URL]] = Def.task {
    implicit val logger = streams.value.log

    val url = (cfnUploadTemplate in aws).value
    logger.info("stack validate: url = " + url)
    val result = validateTemplateOnURL(cfnClient.value)(url)
    result._2 match {
      case Failure(e) => logger.error(s"validation of ${result._1} failed with: \n ${e.getMessage}")
      case _          =>
    }
    if (result._2.isFailure) {
      sys.error("some AWS CloudFormation templates failed to validate!")
    }
    result._1
  }

  private[cfn] def validateTemplateOnFile(client: AmazonCloudFormationClient)(template: sbt.File)(implicit logger: Logger): (File, Try[Seq[String]]) = {
    (template, {
      val request = ValidateTemplateRequestFactory.create().withTemplateBody(IO.read(template))
      client.validateTemplateAsTry(request).map {
        result =>
          logger.debug(s"result from validating $template : $result")
          logger.info(s"validated $template, $result")
          result.parameters.map(_.parameterKeyOpt.get)
      }
    })
  }

  def stackValidateOnFileTask(): Def.Initialize[Task[Seq[sbt.File]]] = Def.task {
    implicit val logger = streams.value.log
    val files = (cfnTemplates in aws).value
    logger.info("stack validate: files = " + files)
    val results = files.map(validateTemplateOnFile(cfnClient.value))
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

  private[cfn] def describeStacks(client: AmazonCloudFormationClient, stackName: String): Try[Seq[Stack]] = {
    val request = DescribeStacksRequestFactory.create().withStackName(stackName)
    client.describeStacksAsTry(request).map { result =>
      result.stacks
    }
  }

  def describeStacksTask(): Def.Initialize[Task[Seq[Stack]]] = Def.task {
    val logger = streams.value.log
    val stackName = (cfnStackName in aws).value
    val client = cfnClient.value

    require(stackName.isDefined)

    stackName.map { sn =>
      logger.info(s"describe stack request: stackName = $sn")
      describeStacks(client, sn).get
    }.map { stacks =>
      if (stacks.isEmpty) {
        logger.info("stacks is empty.")
      } else
        stacks.foreach { stack =>
          logger.info(stack.toString)
        }
      stacks
    }.getOrElse(Seq.empty)
  }

  private[cfn] def getStackStatus(client: AmazonCloudFormationClient, stackName: String): Try[Option[String]] = {
    val request = DescribeStacksRequestFactory.create().withStackName(stackName)
    client.describeStacksAsTry(request).map { result =>
      result.stacks.headOption.flatMap { stack => stack.stackStatusOpt }
    }.recoverWith {
      case ex: AmazonServiceException if ex.getStatusCode == 400 =>
        Success(None)
    }
  }

  def statusStackTask(): Def.Initialize[Task[Option[String]]] = Def.task {
    val logger = streams.value.log
    val stackName = (cfnStackName in aws).value
    val client = cfnClient.value

    stackName.flatMap { sn =>
      logger.info(s"status: stackName = $sn")
      getStackStatus(client, sn).map { statusOpt =>
        statusOpt.map { status =>
          logger.info(s"${sn}'s status is $status")
          Some(status)
        }.getOrElse {
          logger.info(s"$sn does not exists.")
          None
        }
      }.get
    }
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

  private[cfn] def createStack(client: AmazonCloudFormationClient,
                               stackName: String,
                               templateUrl: URL,
                               capabilities: Seq[String],
                               parameters: Map[String, String],
                               tags: Map[String, String])(implicit logger: Logger): Try[CreateStackResult] = {
    val request = CreateStackRequestFactory
      .create()
      .withStackName(stackName)
      .withTemplateURL(templateUrl.toString)
      .withCapabilities(capabilities)
      .withParameters(parameters)
      .withTags(tags)
    client.createStackAsTry(request)
  }

  lazy val cfnStackCapabilitiesTask: Def.Initialize[Task[Seq[String]]] = Def.task {
    if ((cfnCapabilityIam in aws).value)
      (cfnStackCapabilities in aws).value :+ Capability.CAPABILITY_IAM.toString
    else
      (cfnStackCapabilities in aws).value
  }

  def createStackTask(): Def.Initialize[Task[Option[String]]] = Def.task {
    implicit val logger = streams.value.log
    val stackName = (cfnStackName in aws).value
    val client = cfnClient.value

    require(stackName.isDefined)

    stackName.flatMap { sn =>
      describeStacks(client, sn).recover {
        case ex: AmazonServiceException if ex.getStatusCode == 400 =>
          logger.warn(s"occurred exception: message = ${ex.getMessage}, errorCode = ${ex.getErrorCode}")
          Seq.empty
      }.flatMap { stacks =>
        val templateUrl = (cfnUploadTemplate in aws).value
        val capabilities = cfnStackCapabilitiesTask.value
        val params = (cfnStackParams in aws).value
        val tags = (cfnStackTags in aws).value
        stacks.headOption.map { stack =>
          logger.info(s"stack is exists $sn")
          Success(None)
        }.getOrElse {
          logger.info(s"create stack request : stackName = $sn, templateUrl = $templateUrl, capabilities = $capabilities, stackParams = $params, tags = $tags")
          createStack(client, sn, templateUrl, capabilities, params, tags).map(Some(_))
        }
      }.map { resultOpt =>
        resultOpt.flatMap { result =>
          logger.info(s"create stack requested : $sn / ${result.stackIdOpt.get}")
          result.stackIdOpt
        }
      }.get
    }
  }

  def createStackAndWaitTask(): Def.Initialize[Task[Option[String]]] = Def.task {
    implicit val logger = streams.value.log
    val client = cfnClient.value
    val interval = (poolingInterval in aws).value

    (cfnStackCreate in aws).value.flatMap { sn =>
      val (progressStatuses, headOption) = waitStack(client, sn)
      progressStatuses.foreach { s =>
        logger.info(s"status = $s")
        Thread.sleep(interval)
      }
      headOption()
    }
  }

  private[cfn] def deleteStack(client: AmazonCloudFormationClient,
                               stackName: String)(implicit logger: Logger): Try[Unit] = {
    val request = DeleteStackRequestFactory.create().withStackName(stackName)
    client.deleteStackAsTry(request)
  }

  def deleteStackTask(): Def.Initialize[Task[Option[String]]] = Def.task {
    implicit val logger = streams.value.log
    val stackName = (cfnStackName in aws).value
    val client = cfnClient.value

    require(stackName.isDefined)

    stackName.flatMap { sn =>
      describeStacks(client, sn).recover {
        case ex: AmazonServiceException if ex.getStatusCode == 400 =>
          logger.warn(s"occurred exception: message = ${ex.getMessage}, errorCode = ${ex.getErrorCode}")
          Seq.empty
      }.flatMap { stacks =>
        stacks.headOption.map { stack =>
          logger.info(s"delete stack request : stackName = $sn")
          deleteStack(client, sn).map(_ => Some(sn))
        }.getOrElse {
          logger.info(s"does not exists $sn")
          Success(None)
        }
      }.map { resultOpt =>
        resultOpt.map { _ =>
          logger.info(s"delete stack requested : $sn")
          sn
        }
      }.get
    }
  }

  def deleteStackAndWaitTask(): Def.Initialize[Task[Option[String]]] = Def.task {
    implicit val logger = streams.value.log
    val client = cfnClient.value
    val interval = (poolingInterval in aws).value

    (cfnStackDelete in aws).value.flatMap { sn =>
      val (progressStatuses, headOption) = waitStack(client, sn)
      progressStatuses.foreach { s =>
        logger.info(s"status = $s")
        Thread.sleep(interval)
      }
      headOption()
    }

  }

  private[cfn] def updateStack(client: AmazonCloudFormationClient,
                               stackName: String,
                               templateUrl: URL,
                               capabilities: Seq[String],
                               parameters: Map[String, String])(implicit logger: Logger): Try[Option[UpdateStackResult]] = {
    val request = UpdateStackRequestFactory
      .create()
      .withStackName(stackName)
      .withTemplateURL(templateUrl.toString())
      .withCapabilities(capabilities)
      .withParameters(parameters)
    client.updateStackAsTry(request).map(e => Some(e)).recoverWith {
      case ex: AmazonServiceException if ex.getStatusCode == 400 =>
        logger.warn(s"occurred exception: message = ${ex.getMessage}, errorCode = ${ex.getErrorCode}")
        Success(None)
    }
  }

  def updateStackTask(): Def.Initialize[Task[Option[String]]] = Def.task {
    implicit val logger = streams.value.log
    val stackName = (cfnStackName in aws).value
    val client = cfnClient.value

    require(stackName.isDefined)

    stackName.flatMap { sn =>
      describeStacks(client, sn).flatMap { stacks =>
        val templateUrl = (cfnUploadTemplate in aws).value
        val capabilities = cfnStackCapabilitiesTask.value
        val params = (cfnStackParams in aws).value
        stacks.headOption.map { stack =>
          logger.info(s"update stack request : stackName = $sn, templateUrl = $templateUrl, capabilities = $capabilities, stackParams = $params")
          updateStack(client, sn, templateUrl, capabilities, params)
        }.getOrElse {
          logger.info("No updates are to be performed.")
          Success(None)
        }
      }.map { resultOpt =>
        resultOpt.flatMap { result =>
          logger.info(s"update stack requested : $sn / ${result.stackIdOpt.get}")
          result.stackIdOpt
        }
      }.get
    }
  }

  def updateStackAndWaitTask(): Def.Initialize[Task[Option[String]]] = Def.task {
    implicit val logger = streams.value.log
    val client = cfnClient.value
    val interval = (poolingInterval in aws).value

    (cfnStackUpdate in aws).value.flatMap { sn =>
      val (progressStatuses, headOption) = waitStack(client, sn)
      progressStatuses.foreach {
        s =>
          logger.info(s"status = $s")
          Thread.sleep(interval)
      }
      headOption()
    }

  }

  def createOrUpdateStackTask(): Def.Initialize[Task[Option[String]]] = Def.task {
    implicit val logger = streams.value.log
    val client = cfnClient.value
    val stackName = (cfnStackName in aws).value

    require(stackName.isDefined)

    stackName.flatMap { sn =>
      describeStacks(client, sn).recover {
        case ex: AmazonServiceException if ex.getStatusCode == 400 =>
          logger.warn(s"occurred exception: message = ${ex.getMessage}, errorCode = ${ex.getErrorCode}")
          Seq.empty
      }.flatMap { stacks =>
        val templateUrl = (cfnUploadTemplate in aws).value
        val capabilities = cfnStackCapabilitiesTask.value
        val params = (cfnStackParams in aws).value
        val tags = (cfnStackTags in aws).value
        stacks.headOption.map { stack =>
          logger.info(s"update stack request : stackName = $sn, templateUrl = $templateUrl, capabilities = $capabilities, stackParams = $params")
          updateStack(client, sn, templateUrl, capabilities, params)
        }.getOrElse {
          logger.info(s"create stack request : stackName = $sn, templateUrl = $templateUrl, capabilities = $capabilities, stackParams = $params, tags = $tags")
          createStack(client, sn, templateUrl, capabilities, params, tags).map { result => println(result.toString); result }
        }
      }.map {
        case result: CreateStackResult =>
          logger.info(s"created stack requested : $sn / ${result.stackIdOpt.get}")
          result.stackIdOpt
        case resultOpt: Option[_] =>
          resultOpt.map {
            case result: UpdateStackResult =>
              logger.info(s"updated stack requested : $sn / ${result.stackIdOpt.get}")
              result.stackIdOpt
          }.getOrElse {
            logger.info("No updates are to be performed.")
            None
          }
      }.get
    }
  }

  def createOrUpdateStackAndWaitTask(): Def.Initialize[Task[Option[String]]] = Def.task {
    implicit val logger = streams.value.log
    val client = cfnClient.value
    val interval = (poolingInterval in aws).value

    (cfnStackCreateOrUpdate in aws).value.flatMap { sn =>
      val (progressStatuses, headOption) = waitStack(client, sn)
      progressStatuses.foreach {
        s =>
          logger.info(s"status = $s")
          Thread.sleep(interval)
      }
      headOption()
    }

  }

  private[cfn] def waitStack(client: AmazonCloudFormationClient,
                             stackName: String)(implicit logger: Logger) = {
    def statuses: Stream[String] = Stream.cons(getStackStatus(client, stackName).get.getOrElse(""), statuses)

    val progressStatuses: Stream[String] = statuses.takeWhile { status => status.endsWith("_PROGRESS") }
    (progressStatuses, () => statuses.headOption)
  }

  def waitStackTask(): Def.Initialize[Task[Option[String]]] = Def.task {
    implicit val logger = streams.value.log
    val client = cfnClient.value
    val stackName = (cfnStackName in aws).value
    val interval = (poolingInterval in aws).value

    require(stackName.isDefined)

    stackName.flatMap { sn =>
      val (progressStatuses, headOption) = waitStack(client, sn)
      progressStatuses.foreach {
        s =>
          Thread.sleep(interval)
      }
      headOption()
    }

  }
}

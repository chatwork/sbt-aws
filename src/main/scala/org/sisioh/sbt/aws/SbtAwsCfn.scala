package org.sisioh.sbt.aws

import java.io.FileNotFoundException

import com.amazonaws.AmazonServiceException
import com.amazonaws.regions.Region
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient
import com.amazonaws.services.cloudformation.model.Stack
import AwsKeys.CfnKeys._
import AwsKeys._
import org.sisioh.aws4s.cfn.Implicits._
import org.sisioh.aws4s.cfn.model.{DescribeStacksRequestFactory, ValidateTemplateRequestFactory}
import sbt.Keys._
import sbt._

import scala.util.{Success, Failure, Try}

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
    results.foreach{ tr =>
        tr._2 match {
          case Failure(e) => logger.error(s"validation of ${tr._1} failed with: \n ${e.getMessage}")
          case _ =>
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
    val result = cfnClient.value.describeStacksAsTry(request).map{ result =>
      result.stacks
    }.recoverWith{
      case ex: AmazonServiceException if ex.getStatusCode == 400 =>
        Success(Seq.empty)
    }
    result.foreach{ stacks =>
      stacks.foreach(stack => logger.info(stack.toString))
    }
    result.get
  }
}
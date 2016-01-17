package com.chatwork.sbt.aws.eb

import java.io.{ File, FileWriter }
import java.text.SimpleDateFormat
import java.util.Date

import com.amazonaws.regions.Region
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient
import com.amazonaws.services.elasticbeanstalk.model.{ RestartAppServerRequest, ConfigurationOptionSetting, S3Location, SolutionStackDescription }
import com.chatwork.sbt.aws.core.SbtAwsCoreKeys._
import com.chatwork.sbt.aws.eb.SbtAwsEbKeys._
import com.chatwork.sbt.aws.s3.SbtAwsS3
import org.sisioh.aws4s.eb.Implicits._
import org.sisioh.aws4s.eb.model.{ DescribeConfigurationSettingsRequestFactory, DescribeEventsRequestFactory, S3LocationFactory, ValidateConfigurationSettingsRequestFactory }
import sbt.Keys._
import sbt._

import scala.collection.JavaConverters._

object SbtAwsEb extends SbtAwsEb

trait SbtAwsEb
    extends SbtAwsS3
    with ApplicationSupport
    with ApplicationVersionSupport
    with EnvironmentSupport
    with ConfigurationTemplateSupport {

  private[eb] val waitingIntervalInSec = 5L

  private[eb] def wait[T](client: AWSElasticBeanstalkClient)(call: => T)(break: (T) => Boolean)(implicit logger: Logger) = {
    def statuses: Stream[T] = Stream.cons(call, statuses)
    val progressStatuses: Stream[T] = statuses.takeWhile { e =>
      !break(e)
    }
    (progressStatuses, () => statuses.headOption)
  }

  lazy val ebClient = Def.task {
    val logger = streams.value.log
    val r = (region in aws).value
    val cpn = (credentialProfileName in aws).value
    logger.debug(s"region = $r, credentialProfileName = $cpn")
    createClient(classOf[AWSElasticBeanstalkClient], Region.getRegion(r), cpn)
  }

  def ebBuildBundleTask(): Def.Initialize[Task[File]] = Def.task {
    val logger = streams.value.log
    val files = (ebBundleTargetFiles in aws).value
    val path = baseDirectory.value / "target" / (ebBundleFileName in aws).value
    logger.info(s"create application-bundle: $path")
    IO.zip(files, path)
    logger.info(s"created application-bundle: $path")
    path
  }

  def timestamp: String = {
    val sdf = new SimpleDateFormat("yyyyMMdd'_'HHmmss")
    sdf.format(new Date())
  }

  def ebUploadBundleTask(): Def.Initialize[Task[S3Location]] = Def.task {
    val logger = streams.value.log
    val path = (ebBuildBundle in aws).value
    val createBucket = (ebS3CreateBucket in aws).value
    val projectName = (name in thisProjectRef).value
    val projectVersion = (version in thisProjectRef).value
    val bucketName = (ebS3BucketName in aws).value
    val keyMapper = (ebS3KeyMapper in aws).value
    val versionLabel = (ebApplicationVersionLabel in aws).value

    require(bucketName.isDefined)

    val baseKey = s"$projectName/$projectName-$versionLabel.zip"
    val key = keyMapper(baseKey)

    val overwrite = projectVersion.endsWith("-SNAPSHOT")

    logger.info(s"upload application-bundle : $path to ${bucketName.get}/$key")
    s3PutObject(s3Client.value, bucketName.get, key, path, overwrite, createBucket).get
    logger.info(s"uploaded application-bundle : ${bucketName.get}/$key")

    S3LocationFactory.create().withS3Bucket(bucketName.get).withS3Key(key)
  }

  def ebGenerateFilesInBundleTask(): Def.Initialize[Task[Seq[File]]] = Def.task {
    val logger = streams.value.log
    val src = (ebBundleDirectory in aws).value
    if (src.exists()) {
      val cfg = new freemarker.template.Configuration(freemarker.template.Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS)
      cfg.setDirectoryForTemplateLoading(src)

      val context = (ebBundleContext in aws).value.asJava

      val templates = (ebTargetTemplates in aws).value

      templates.map {
        case (templatePath, outputFile) =>
          var writer: FileWriter = null
          try {
            val template = cfg.getTemplate(templatePath)
            writer = new FileWriter(outputFile)
            template.process(context, writer)
            logger.info("generated files in the bundle.")
            outputFile
          } finally {
            if (writer != null)
              writer.close()
          }
      }.toSeq
    } else {
      logger.warn(s"${src.getAbsolutePath} is not found.")
      Seq.empty
    }
  }

  private[eb] def validateConfigurationSettings(client: AWSElasticBeanstalkClient, applicationName: String, configurationOptionSettings: Seq[ConfigurationOptionSetting]) = {
    val request = ValidateConfigurationSettingsRequestFactory
      .create()
      .withApplicationName(applicationName)
      .withOptionSettings(configurationOptionSettings)
    client.validateConfigurationSettingsAsTry(request)
  }

  private[eb] def ebDescribeEvents(client: AWSElasticBeanstalkClient, applicationName: String, envrionmentName: Option[String], templateName: Option[String])(implicit logger: Logger) = {
    val request = DescribeEventsRequestFactory
      .create()
      .withApplicationName(applicationName)
      .withEnvironmentNameOpt(envrionmentName)
      .withTemplateNameOpt(templateName)
    val result = client.describeEventsAsTry(request)
    result
  }

  private[eb] def ebDescribeConfigurationSettings(client: AWSElasticBeanstalkClient, applicationName: String, environmentName: Option[String], templateName: Option[String])(implicit logger: Logger) = {
    logger.debug(s"describe configurationSettings start: $applicationName, $environmentName, $templateName")
    val request = DescribeConfigurationSettingsRequestFactory
      .create(applicationName)
      .withEnvironmentNameOpt(environmentName)
      .withTemplateNameOpt(templateName)
    val result = client.describeConfigurationSettingsAsTry(request)
    logger.debug(s"describe configurationSettings finish: $applicationName, $environmentName, $templateName")
    result
  }

  private[eb] def ebListAvailableSolutionStacks(client: AWSElasticBeanstalkClient)(implicit logger: Logger) = {
    client.listAvailableSolutionStacksAsTry().map { result =>
      result.getSolutionStackDetails.asScala.map { detail =>
        logger.info(s"${detail.getSolutionStackName}, ${detail.getPermittedFileTypes.asScala.mkString("{ ", ", ", " }")}")
        detail
      }
    }
  }

  def ebListAvailableSolutionStacksTask(): Def.Initialize[Task[Seq[SolutionStackDescription]]] = Def.task {
    implicit val logger = streams.value.log
    ebListAvailableSolutionStacks(ebClient.value).get
  }

}

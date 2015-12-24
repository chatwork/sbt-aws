package com.chatwork.sbt.aws.eb

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

import com.amazonaws.regions.Region
import com.amazonaws.services.elasticbeanstalk.AWSElasticBeanstalkClient
import com.amazonaws.services.elasticbeanstalk.model.S3Location
import com.chatwork.sbt.aws.core.SbtAwsCoreKeys._
import com.chatwork.sbt.aws.eb.SbtAwsEbKeys._
import com.chatwork.sbt.aws.s3.SbtAwsS3
import org.sisioh.aws4s.eb.Implicits._
import org.sisioh.aws4s.eb.model.{ S3LocationFactory, DescribeEventsRequestFactory, DescribeConfigurationSettingsRequestFactory }
import sbt.Keys._
import sbt._

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
    logger.info(s"region = $r, credentialProfileName = $cpn")
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

  def ebUploadBundleTask(): Def.Initialize[Task[S3Location]] = Def.task {
    val logger = streams.value.log
    val path = (ebBuildBundle in aws).value
    val createBucket = (ebS3CreateBucket in aws).value
    val projectName = (name in thisProjectRef).value
    val projectVersion = (version in thisProjectRef).value
    val bucketName = (ebS3BucketName in aws).value
    val keyMapper = (ebS3KeyMapper in aws).value

    require(bucketName.isDefined)

    val sdf = new SimpleDateFormat("yyyyMMdd'_'HHmmss")
    val timestamp = sdf.format(new Date())

    val baseKey = s"$projectName/$projectName-$projectVersion-$timestamp.zip"
    val key = keyMapper(baseKey)

    val overwrite = projectVersion.endsWith("-SNAPSHOT")

    logger.info(s"upload application-bundle : $path to ${bucketName.get}/$key")
    s3PutObject(s3Client.value, bucketName.get, key, path, overwrite, createBucket).get
    logger.info(s"uploaded application-bundle : ${bucketName.get}/$key")

    S3LocationFactory.create().withS3Bucket(bucketName.get).withS3Key(key)
  }

  private[eb] def ebDescribeEvents(client: AWSElasticBeanstalkClient, applicationName: String, envrionmentName: Option[String], templateName: Option[String])(implicit logger: Logger) = {
    logger.info(s"describe event start: $applicationName, $envrionmentName, $templateName")
    val request = DescribeEventsRequestFactory
      .create()
      .withApplicationName(applicationName)
      .withEnvironmentNameOpt(envrionmentName)
      .withTemplateNameOpt(templateName)
    val result = client.describeEventsAsTry(request)
    logger.info(s"describe event finish: $applicationName, $envrionmentName, $templateName")
    result
  }

  private[eb] def ebDescribeConfigurationSettings(client: AWSElasticBeanstalkClient, applicationName: String, envrionmentName: Option[String], templateName: Option[String])(implicit logger: Logger) = {
    logger.info(s"describe configurationSettings start: $applicationName, $envrionmentName, $templateName")
    val request = DescribeConfigurationSettingsRequestFactory
      .create(applicationName)
      .withEnvironmentNameOpt(envrionmentName)
      .withTemplateNameOpt(templateName)
    val result = client.describeConfigurationSettingsAsTry(request)
    logger.info(s"describe configurationSettings finish: $applicationName, $envrionmentName, $templateName")
    result
  }

}

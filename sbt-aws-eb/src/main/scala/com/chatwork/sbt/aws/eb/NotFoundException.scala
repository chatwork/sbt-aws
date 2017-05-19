package com.chatwork.sbt.aws.eb

abstract class NotFoundException(message: String, cause: Option[Throwable] = None)
    extends Exception(message, cause.orNull)

case class ApplicationNotFoundException(message: String, cause: Option[Throwable] = None)
    extends Exception(message, cause.orNull)

case class ApplicationVersionNotFoundException(message: String, cause: Option[Throwable] = None)
    extends Exception(message, cause.orNull)

case class EnvironmentNotFoundException(message: String, cause: Option[Throwable] = None)
    extends Exception(message, cause.orNull)

case class ConfigurationTemplateNotFoundException(message: String, cause: Option[Throwable] = None)
    extends Exception(message, cause.orNull)

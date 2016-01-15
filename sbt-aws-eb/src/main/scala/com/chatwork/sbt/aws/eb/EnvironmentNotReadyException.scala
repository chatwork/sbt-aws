package com.chatwork.sbt.aws.eb

case class EnvironmentNotReadyException(message: String, cause: Option[Throwable] = None) extends Exception(message, cause.orNull)

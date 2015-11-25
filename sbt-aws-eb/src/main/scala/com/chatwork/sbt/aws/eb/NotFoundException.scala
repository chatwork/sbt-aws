package com.chatwork.sbt.aws.eb

case class NotFoundException(message: String, cause: Option[Throwable] = None) extends Exception(message, cause.orNull)

package com.chatwork.sbt.aws.eb

case class AlreadyExistsException(message: String, cause: Option[Throwable] = None)
    extends Exception(message, cause.orNull)

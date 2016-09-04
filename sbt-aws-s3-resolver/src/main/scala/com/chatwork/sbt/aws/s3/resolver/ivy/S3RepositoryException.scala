package com.chatwork.sbt.aws.s3.resolver.ivy

case class S3RepositoryException(throwable: Throwable) extends Exception(throwable)

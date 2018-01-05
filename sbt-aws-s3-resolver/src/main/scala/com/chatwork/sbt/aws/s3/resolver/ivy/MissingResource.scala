package com.chatwork.sbt.aws.s3.resolver.ivy

import java.io.{ByteArrayInputStream, InputStream}

import org.apache.ivy.plugins.repository.Resource

case class MissingResource() extends Resource {
  override def getName: String = ""

  override def getLastModified: Long = 0L

  override def getContentLength: Long = 0L

  override def openStream(): InputStream = new ByteArrayInputStream("".getBytes())

  override def isLocal: Boolean = false

  override def clone(cloneName: String): Resource = copy()

  override def exists(): Boolean = false
}

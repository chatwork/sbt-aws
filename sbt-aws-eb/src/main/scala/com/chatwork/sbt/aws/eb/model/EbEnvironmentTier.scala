package com.chatwork.sbt.aws.eb.model

import com.amazonaws.services.elasticbeanstalk.model.EnvironmentTier

case class EbEnvironmentTier(name: String, typeName: String, version: Option[String])
    extends EnvironmentTier {
  setName(name)
  setType(typeName)
  setVersion(version.orNull)
}

object EbEnvironmentTier {

  object WebServer extends EbEnvironmentTier("WebServer", "Standard", Some("1.0"))

  object Worker extends EbEnvironmentTier("Worker", "Standard", Some("1.0"))

}


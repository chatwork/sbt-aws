package com.chatwork.sbt.aws.eb

import com.amazonaws.services.elasticbeanstalk.model.{ ConfigurationOptionSetting, EnvironmentTier, OptionSpecification, Tag }

trait Models {

  case class EbOptionSpecification(namespace: String, optionName: String) extends OptionSpecification {
    setNamespace(namespace)
    setOptionName(optionName)
  }

  case class EbConfigurationOptionSetting(namespace: String,
                                          optionName: String,
                                          value: String)
      extends ConfigurationOptionSetting(namespace, optionName, value)

  case class EbTag(key: String, value: String) extends Tag {
    setKey(key)
    setValue(value)
  }

  case class EbConfigurationTemplate(name: String,
                                     description: Option[String],
                                     solutionStackName: String,
                                     optionSettings: Seq[EbConfigurationOptionSetting],
                                     optionsToRemoves: Seq[EbOptionSpecification],
                                     recreate: Boolean)

  sealed case class EbEnvironmentTier(name: String, typeName: String, version: Option[String])
      extends EnvironmentTier {
    setName(name)
    setType(typeName)
    setVersion(version.orNull)
  }

  object EbEnvironmentTier {

    object WebServer extends EbEnvironmentTier("WebServer", "Standard", Some("1.0"))

    object Worker extends EbEnvironmentTier("Worker", "SQS/HTTP", Some("1.0"))

  }

}


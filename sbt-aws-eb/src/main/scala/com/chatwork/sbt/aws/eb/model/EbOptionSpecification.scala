package com.chatwork.sbt.aws.eb.model

import com.amazonaws.services.elasticbeanstalk.model.OptionSpecification

case class EbOptionSpecification(namespace: String, optionName: String) extends OptionSpecification {
  setNamespace(namespace)
  setOptionName(optionName)
}

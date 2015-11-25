package com.chatwork.sbt.aws.eb.model

import com.amazonaws.services.elasticbeanstalk.model.Tag

case class EbTag(key: String, value: String) extends Tag {
  setKey(key)
  setValue(value)
}

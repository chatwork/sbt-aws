package com.chatwork.sbt.aws.eb.model

import com.amazonaws.services.elasticbeanstalk.model.ConfigurationOptionSetting

case class EbConfigurationOptionSetting(namespace: String,
                                        optionName: String,
                                        value: String)
    extends ConfigurationOptionSetting(namespace, optionName, value)

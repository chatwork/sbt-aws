package com.chatwork.sbt.aws.eb.model

case class EbConfigurationTemplate(name: String,
                                   description: String,
                                   solutionStackName: String,
                                   optionSettings: Seq[EbConfigurationOptionSetting],
                                   optionsToRemoves: Seq[EbOptionSpecification],
                                   recreate: Boolean)

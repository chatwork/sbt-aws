package com.chatwork.sbt.aws.core

import com.amazonaws.regions.Regions
import sbt._
import org.sisioh.config.{ Configuration => SisiohConfiguration }

trait SbtAwsCoreKeys {
  lazy val aws = taskKey[Unit]("aws")

  lazy val region = settingKey[Regions]("region")

  lazy val credentialProfileName = settingKey[Option[String]]("credential-profile-name")

  lazy val environmentName = settingKey[String]("env")

  lazy val configFileFolder = settingKey[File]("config-folder")

  lazy val configFile = settingKey[File]("config-file")

  lazy val config = settingKey[SisiohConfiguration]("config")

  lazy val awsConfig = settingKey[SisiohConfiguration]("aws-config")

  lazy val poolingInterval = settingKey[Int]("pooling-interval")
}

object SbtAwsCoreKeys extends SbtAwsCoreKeys
package com.chatwork.sbt.aws.core

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{
  AWSCredentialsProviderChain,
  EnvironmentVariableCredentialsProvider,
  InstanceProfileCredentialsProvider,
  SystemPropertiesCredentialsProvider
}
import com.amazonaws.regions.Regions
import sbt._
import org.sisioh.config.{Configuration => SisiohConfiguration}
import sbt.plugins.IvyPlugin

object SbtAwsCorePlugin extends AutoPlugin {

  override def trigger = allRequirements

  override def requires: Plugins = IvyPlugin

  object autoImport extends SbtAwsCoreKeys

  import SbtAwsCoreKeys._

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    credentialProfileName in aws := None,
    credentialsProviderChain in aws := new AWSCredentialsProviderChain(
      new EnvironmentVariableCredentialsProvider(),
      new SystemPropertiesCredentialsProvider(),
      new ProfileCredentialsProvider((credentialProfileName in aws).value.orNull),
      new InstanceProfileCredentialsProvider()
    ),
    region in aws := Regions.AP_NORTHEAST_1,
    environmentName in aws := System.getProperty("aws.env", "dev"),
    configFileFolder in aws := file("env"),
    configFile in aws := {
      val parent = (configFileFolder in aws).value
      parent / ((environmentName in aws).value + ".conf")
    },
    config in aws := {
      SisiohConfiguration.parseFile((configFile in aws).value).toOption
        .getOrElse(SisiohConfiguration.empty)
    },
    awsConfig in aws := {
      (config in aws).value.getConfiguration(aws.key.label).getOrElse(SisiohConfiguration.empty)
    },
    poolingInterval in aws := 1000,
    clientConfiguration in aws := {
      val clientConfiguration = new ClientConfiguration()
      clientConfiguration.setConnectionTimeout(15 * 1000)
      Some(clientConfiguration)
    }
  )

}

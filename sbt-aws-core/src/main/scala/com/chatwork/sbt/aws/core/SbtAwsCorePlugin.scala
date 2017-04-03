package com.chatwork.sbt.aws.core

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{AWSCredentialsProviderChain, EnvironmentVariableCredentialsProvider, InstanceProfileCredentialsProvider, SystemPropertiesCredentialsProvider}
import com.amazonaws.regions.Regions
import org.sisioh.config.{Configuration => SisiohConfiguration}
import sbt._
import sbt.plugins.IvyPlugin

object SbtAwsCorePlugin extends AutoPlugin {

  override def trigger = allRequirements

  override def requires: Plugins = IvyPlugin

  object autoImport extends SbtAwsCoreKeys

  import autoImport._

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    credentialProfileName in aws := None,
    defaultCredentialsProviderChain in aws := { (credentialProfileName: Option[String]) =>
      new AWSCredentialsProviderChain(
        new EnvironmentVariableCredentialsProvider(),
        new SystemPropertiesCredentialsProvider(),
        new ProfileCredentialsProvider(credentialProfileName.orNull),
        new InstanceProfileCredentialsProvider()
      )
    },
    profileCredentialsProviderChain in aws := { (credentialProfileName: String) =>
      new AWSCredentialsProviderChain(new ProfileCredentialsProvider(credentialProfileName))
    },
    credentialsProviderChain in aws := (defaultCredentialsProviderChain in aws).value((credentialProfileName in aws).value),
    region in aws := Regions.AP_NORTHEAST_1,
    environmentName in aws := System.getProperty("aws.env", "dev"),
    configFileFolder in aws := file("env"),
    configFile in aws := {
      val parent = (configFileFolder in aws).value
      parent / ((environmentName in aws).value + ".conf")
    },
    config in aws := {
      Option(SisiohConfiguration.parseFile((configFile in aws).value)).getOrElse(SisiohConfiguration.empty)
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

package com.chatwork.sbt.aws.core

import com.amazonaws.regions.Regions
import sbt._
import org.sisioh.config.{ Configuration => SisiohConfiguration }

object SbtAwsCorePlugin extends AutoPlugin {

  override def trigger = allRequirements

  object autoImport extends SbtAwsCoreKeys

  import SbtAwsCoreKeys._

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    credentialProfileName in aws := None,
    region in aws := Regions.AP_NORTHEAST_1,
    environmentName in aws := System.getProperty("aws.env", "dev"),
    configFileFolder in aws := file("env"),
    configFile in aws := {
      val parent = (configFileFolder in aws).value
      val file = parent / ((environmentName in aws).value + ".conf")
      file
    },
    config in aws := {
      Option(SisiohConfiguration.parseFile((configFile in aws).value)).getOrElse(SisiohConfiguration.empty)
    },
    awsConfig in aws := {
      (config in aws).value.getConfiguration(aws.key.label).getOrElse(SisiohConfiguration.empty)
    },
    poolingInterval in aws := 1000
  )

}

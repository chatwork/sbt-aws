import java.io.FileWriter

import scala.collection.JavaConverters._

name := """play-sample-scala"""

version := "1.0.1-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  specs2 % Test
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

assemblyMergeStrategy in assembly := {
  case "log4j.properties" => MergeStrategy.first
  case ".gitkeep" => MergeStrategy.first
  case x if x.startsWith("org/apache/commons/logging/") => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

test in assembly := {}

mainClass in assembly := Some("play.core.server.ProdServerStart")

fullClasspath in assembly += Attributed.blank(PlayKeys.playPackageAssets.value)

credentialProfileName in aws := Some("sbt-aws-scripted-test")

ebBundleTargetFiles in aws := {
  val src = baseDirectory.value / "ebBundle"
  Seq(
    (src / ".ebextensions" / "nginx" / "conf.d" / "elasticbeanstalk" / "00_application.conf",
      ".ebextensions/nginx/conf.d/elasticbeanstalk/00_application.conf"),
    (src / "boot.sh", "boot.sh"),
    (src / "Procfile", "Procfile")
  ) ++ Seq((assemblyOutputPath in assembly).value -> (assemblyJarName in assembly).value)
}

def assemblyJarFile = Def.task {
  val jarFile = assembly.value
  val jarPath = IO.relativize(baseDirectory.value, jarFile).get
  (jarFile, jarPath)
}

ebBuildBundle in aws <<= (ebBuildBundle in aws) dependsOn(assemblyJarFile)

ebS3BucketName in aws := Some("sbt-aws-eb-test")

ebS3CreateBucket in aws := true

ebSolutionStackName in aws := Some("64bit Amazon Linux 2015.09 v2.0.4 running Java 8")

ebConfigurationOptionSettings in aws := {
  Seq(
    ("aws:elb:healthcheck", "Interval", "5"),
    ("aws:elb:healthcheck", "Timeout", "4"),
    ("aws:elb:healthcheck", "HealthyThreshold", "2"),
    ("aws:elb:healthcheck", "UnhealthyThreshold", "10"),
    ("aws:autoscaling:launchconfiguration", "EC2KeyName", "aws-eb"),
    ("aws:autoscaling:asg", "MinSize", "2"),
    ("aws:autoscaling:asg", "MaxSize", "4"),
    ("aws:autoscaling:updatepolicy:rollingupdate", "RollingUpdateEnabled", "true"),
    ("aws:autoscaling:updatepolicy:rollingupdate", "RollingUpdateType", "Health"),
    ("aws:elasticbeanstalk:application", "Application Healthcheck URL", "/"),
    ("aws:autoscaling:trigger", "MeasureName", "CPUUtilization"),
    ("aws:autoscaling:trigger", "Statistic", "Average"),
    ("aws:autoscaling:trigger", "Unit", "Percent"),
    ("aws:autoscaling:trigger", "LowerThreshold", "5"),
    ("aws:autoscaling:trigger", "UpperThreshold", "70"),
    ("aws:rds:dbinstance", "DBAllocatedStorage", "5"),
    ("aws:rds:dbinstance", "DBDeletionPolicy", "Snapshot"),
    ("aws:rds:dbinstance", "DBInstance", "db.t1.micro"),
    ("aws:rds:dbinstance", "DBEngine", "mysql"),
    ("aws:rds:dbinstance", "DBUser", "dbroot"),
    ("aws:rds:dbinstance", "DBPassword", "KmrDmWfS1jC1FL")
  ).map(e => EbConfigurationOptionSetting(e._1, e._2, e._3))
}

ebTargetTemplates in aws := Map(
  ("boot.sh.ftl", (ebBundleDirectory in aws).value / "boot.sh")
)

ebDeploySettings


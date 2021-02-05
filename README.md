# DEPRECATED

# sbt-aws

[![Build Status](https://travis-ci.org/chatwork/sbt-aws.svg)](https://travis-ci.org/chatwork/sbt-aws)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.chatwork/sbt-aws_2.10/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.chatwork/sbt-aws_2.10)
[![Scaladoc](http://javadoc-badge.appspot.com/com.chatwork/sbt-aws.svg?label=scaladoc)](http://javadoc-badge.appspot.com/com.chatwork/sbt-aws_2.10)
[![Reference Status](https://www.versioneye.com/java/com.chatwork:sbt-aws_2.11/reference_badge.svg?style=flat)](https://www.versioneye.com/java/com.chatwork:sbt-aws_2.10/references)

The sbt-aws is a sbt's plugin for AWS.

**sbt-aws is no longer maintained.**<br>
**Please consider migration to other tools, suggested below.**

## Supported Services by sbt-aws's plugins.

Supported sbt versions are 0.13.x and 1.0.x.

- [sbt-aws-cfn / Cloud Formation](sbt-aws-cfn/README.md)
    - https://github.com/Dwolla/sbt-cloudformation-stack
    - https://github.com/pigumergroup/sbt-aws-cloudformation

- [sbt-aws-eb / Elastic Beanstalk](sbt-aws-eb/README.md)
    - There seems no actively-maintained tools.

- [sbt-aws-s3 / S3](sbt-aws-s3/README.md)
    - There seems no actively-maintained tools.

- [sbt-aws-s3-resolver / Sbt's Resolver for S3](sbt-aws-s3-resolver/README.md)
    - https://github.com/frugalmechanic/fm-sbt-s3-resolver

## Common Usage
 
You can modify it for you if you can't used configurations on following.

### Base Configurations for AWS

```scala
// Default AWSCredentialsProviderChain.
credentialsProviderChain in aws := new AWSCredentialsProviderChain(
    new EnvironmentVariableCredentialsProvider(),
    new SystemPropertiesCredentialsProvider(),
    new ProfileCredentialsProvider((credentialProfileName in aws).value.orNull),
    new InstanceProfileCredentialsProvider()
)

// Default AWS creadential profile name in ~/.aws/creadentials
credentialProfileName in aws := None

// Default AWS's Region
region in aws := Regions.AP_NORTHEAST_1
```

### Profile Functions for switching the build settings dynamically

You can switch between the different settings for each environment dynamically using the Profile Functions, if you need it.

```scala
// Environment name with the set value of each environment
environmentName in aws := System.getProperty("aws.env", "dev")

// Folder name that you want to store the settings for each environment
configFileFolder in aws := file("env")

// Configuration file for each environment
configFile in aws := {
    val parent = (configFileFolder in aws).value
    parent / ((environmentName in aws).value + ".conf")
}

// Typesafe Config object that represents the configuration file for each environment
config in aws := {
    Option(SisiohConfiguration.parseFile((configFile in aws).value)).getOrElse(SisiohConfiguration.empty)
}

// Typesafe Config object that represents the AWS of configuration files for each environment
awsConfig in aws := {
    (config in aws).value.getConfiguration(aws.key.label).getOrElse(SisiohConfiguration.empty)
}
```

If you want to use this functions on your build.sbt, Please try as follows. 

- build.sbt

```scala
credentialProfileName in aws := {
    getConfigValueOpt(classOf[String], (awsConfig in aws).value, credentialProfileName.key.label)
}
```

- env/staging.conf

```javascript
"aws": {
    "credentialProfileName": "@staging"
}
```

Execute sbt's command, your configurations will modified dynamically by this functions.

```sh
$ sbt -Daws.env=staging aws::aws::CfnStackCreateOrUpdateAndWait
```

  

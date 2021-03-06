package com.chatwork.sbt.aws.s3.resolver

import sbt._

object SbtAwsS3ResolverKeys extends SbtAwsS3ResolverKeys

trait SbtAwsS3ResolverKeys {
  import com.chatwork.sbt.aws.s3.resolver.SbtAwsS3ResolverPlugin.autoImport._
  type AWSCredentialsProvider = com.amazonaws.auth.AWSCredentialsProvider
  lazy val s3Handler = SettingKey[Unit]("s3UrlHanlder", "initialize s3 url handler")
  lazy val s3Resolver = SettingKey[(String, String) => Resolver](
    "s3resolver",
    "Takes name and bucket url and returns an S3 resolver")
  lazy val s3DeployStyle = settingKey[DeployStyle.Value]("s3DeployStyle")
  lazy val showS3Credentials = TaskKey[Unit](
    "showS3Credentials",
    "Just outputs credentials that are loaded by the s3credentials provider")

}

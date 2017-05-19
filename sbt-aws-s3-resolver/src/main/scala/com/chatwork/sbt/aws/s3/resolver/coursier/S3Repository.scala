package com.chatwork.sbt.aws.s3.resolver.coursier

import coursier.Fetch.Content
import coursier.Repository
import coursier.core.{Artifact, Module, Project}

import scalaz.{EitherT, Monad}

final case class S3Repository() extends Repository {

  override def find[F[_]](module: Module, version: String, fetch: Content[F])(
      implicit F: Monad[F]): EitherT[F, String, (Artifact.Source, Project)] = ???

}

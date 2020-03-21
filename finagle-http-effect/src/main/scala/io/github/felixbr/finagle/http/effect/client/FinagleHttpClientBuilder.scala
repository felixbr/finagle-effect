package io.github.felixbr.finagle.http.effect.client

import cats.effect._
import com.twitter.finagle.{Http, Name, Service}
import com.twitter.finagle.Http.Client
import com.twitter.finagle.http.{Request, Response}
import io.github.felixbr.finagle.core.effect.WrappedService

import scala.concurrent.duration._

object FinagleHttpClientBuilder {

  def apply[F[_]: Sync: Async: ContextShift]: FinagleHttpClientBuilder[F] =
    new FinagleHttpClientBuilder[F]
}

final case class FinagleHttpClientBuilder[F[_]: Sync: Async: ContextShift](
  private val underlyingBuilder: Client = Http.client,
  private val closeTimeout: FiniteDuration = 1.second
) {

  def withUpdatedConfig(f: Client => Client): FinagleHttpClientBuilder[F] =
    copy(underlyingBuilder = f(underlyingBuilder))

  def withCloseTimeout(duration: FiniteDuration): FinagleHttpClientBuilder[F] =
    copy(closeTimeout = duration)

  def serviceResource(
    dest: String,
  ): Resource[F, WrappedService[F, Request, Response]] =
    makeServiceResource(underlyingBuilder.newService(dest), closeTimeout)

  def serviceResource(
    dest: String,
    label: String,
  ): Resource[F, WrappedService[F, Request, Response]] =
    makeServiceResource(underlyingBuilder.newService(dest, label), closeTimeout)

  def serviceResource(
    dest: Name,
    label: String,
  ): Resource[F, WrappedService[F, Request, Response]] =
    makeServiceResource(underlyingBuilder.newService(dest, label), closeTimeout)

  private def makeServiceResource(
    newService: Service[Request, Response],
    closeTimeout: FiniteDuration
  ): Resource[F, WrappedService[F, Request, Response]] =
    Resource.make {
      Sync[F].delay {
        new WrappedService[F, Request, Response](newService)
      }
    }(service => service.close(closeTimeout))
}

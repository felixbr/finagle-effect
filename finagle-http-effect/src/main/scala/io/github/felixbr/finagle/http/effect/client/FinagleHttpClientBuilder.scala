package io.github.felixbr.finagle.http.effect.client

import cats.effect._
import cats.syntax.functor._
import com.twitter.finagle.Http.Client
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Http, Name, Service}
import io.github.felixbr.finagle.core.effect.WrappedService

import scala.concurrent.duration._

object FinagleHttpClientBuilder {

  def apply[F[_]: Sync: Async: ContextShift]: FinagleHttpClientBuilder[F] =
    new FinagleHttpClientBuilder[F](Sync[F].delay(Http.client), closeTimeout = 1.second)
}

final private[client] case class FinagleHttpClientBuilder[F[_]: Sync: Async: ContextShift](
  private val underlyingBuilder: F[Client],
  private val closeTimeout: FiniteDuration
) {

  def withUpdatedConfig(f: Client => Client): FinagleHttpClientBuilder[F] =
    copy(underlyingBuilder = underlyingBuilder.map(f))

  def withCloseTimeout(duration: FiniteDuration): FinagleHttpClientBuilder[F] =
    copy(closeTimeout = duration)

  def serviceResource(
    dest: String,
  ): Resource[F, WrappedService[F, Request, Response]] =
    makeServiceResource(
      underlyingBuilder.map(_.newService(dest)),
      closeTimeout
    )

  def serviceResource(
    dest: String,
    label: String,
  ): Resource[F, WrappedService[F, Request, Response]] =
    makeServiceResource(
      underlyingBuilder.map(_.newService(dest, label)),
      closeTimeout
    )

  def serviceResource(
    dest: Name,
    label: String,
  ): Resource[F, WrappedService[F, Request, Response]] =
    makeServiceResource(
      underlyingBuilder.map(_.newService(dest, label)),
      closeTimeout
    )

  private def makeServiceResource(
    newService: F[Service[Request, Response]],
    closeTimeout: FiniteDuration
  ): Resource[F, WrappedService[F, Request, Response]] =
    Resource.make {
      newService.map(new WrappedService[F, Request, Response](_))
    }(service => service.close(closeTimeout))
}

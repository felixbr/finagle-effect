package io.github.felixbr.finagle.http.effect.client

import cats.effect._
import com.twitter.finagle.Http
import com.twitter.finagle.Http.Client
import com.twitter.finagle.http.{Request, Response}
import io.github.felixbr.finagle.http.effect.WrappedService

import scala.concurrent.duration._

object FinagleHttpClientBuilder {

  def apply[F[_]: Sync: Async: ContextShift]: FinagleHttpClientBuilder[F] =
    new FinagleHttpClientBuilder[F]
}

final case class FinagleHttpClientBuilder[F[_]: Sync: Async: ContextShift](
  private val underlyingBuilder: Client = Http.client
) {

  def withUpdatedConfig(f: Client => Client): FinagleHttpClientBuilder[F] =
    copy(underlyingBuilder = f(underlyingBuilder))

  def serviceResource(
    dest: String,
    closeTimeout: FiniteDuration = 1.second
  ): Resource[F, WrappedService[F, Request, Response]] =
    Resource.make {
      Sync[F].delay {
        new WrappedService[F, Request, Response](
          underlyingBuilder.newService(dest)
        )
      }
    }(service => service.close(closeTimeout))
}

package io.github.felixbr.finagle.core.effect

import cats.effect._
import cats.syntax.all._
import com.twitter.finagle.Service

import scala.concurrent.duration._

final class WrappedService[F[_]: Async: ContextShift, Req, Rep](underlying: Service[Req, Rep])
    extends TwitterDurationConversions
    with TwitterFutureConvertersTo[F] {

  def apply(request: Req): F[Rep] =
    futureToF(
      underlying.apply(request)
    )

  def close(deadline: FiniteDuration): F[Unit] =
    futureToF(
      underlying.close(deadline)
    )
}

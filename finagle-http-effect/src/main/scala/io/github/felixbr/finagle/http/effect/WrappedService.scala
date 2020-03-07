package io.github.felixbr.finagle.http.effect

import cats.effect._
import cats.syntax.all._
import com.twitter.finagle.Service
import com.twitter.util.Future
import io.github.felixbr.finagle.http.effect.DurationConversions._

import scala.concurrent.duration._

final class WrappedService[F[_]: Async: ContextShift, Req, Res](underlying: Service[Req, Res]) {

  def apply(request: Req): F[Res] =
    futureToF(
      underlying.apply(request)
    )

  def close(deadline: FiniteDuration): F[Unit] =
    futureToF(
      underlying.close(deadline)
    )

  /**
    * `futureToAsync` does not shift threadpools correctly, so we patch this here
    *
    * see: https://github.com/travisbrown/catbird/pull/209
    */
  private def futureToF[A](fa: => Future[A]): F[A] = {
    import io.catbird.util.effect.futureToAsync

    Bracket[F, Throwable].guarantee { // think `finally`
      futureToAsync[F, A](fa)
    }(ContextShift[F].shift) // Shift back to the normal threadpool (usually from IOApp)
  }
}

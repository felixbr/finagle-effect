package io.github.felixbr.finagle.core.effect

import cats.effect.{Async, Bracket, ContextShift, IO}
import com.twitter.util.Future

import scala.language.implicitConversions

trait TwitterFutureConverters {

  /**
    * `futureToAsync` does not shift threadpools correctly, so we patch this here
    *
    * see: https://github.com/travisbrown/catbird/pull/209
    */
  def futureToF[F[_]: Async: ContextShift, A](fa: => Future[A]): F[A] = {
    import io.catbird.util.effect.futureToAsync

    Bracket[F, Throwable].guarantee { // think `finally`
      futureToAsync[F, A](fa)
    }(ContextShift[F].shift) // Shift back to the normal threadpool (usually from IOApp)
  }

  def futureToIO[A](fa: => Future[A])(implicit CS: ContextShift[IO]): IO[A] = futureToF[IO, A](fa)
}

object TwitterFutureConverters extends TwitterFutureConverters

/**
  * Same as `TwitterFutureConverters` but implicitly.
  *
  * If the implicit is not picked up, you can either use `futureToF` or `futureToIO` directly or provide a
  * type annotation to fix the implicit lookup.
  */
trait TwitterFutureConversions extends TwitterFutureConverters {
  implicit def implicitFutureToF[F[_]: Async: ContextShift, A](fa: => Future[A]): F[A] = futureToF[F, A](fa)

  implicit def implicitFutureToIO[A](fa: => Future[A])(implicit cs: ContextShift[IO]): IO[A] = futureToIO(fa)
}

object TwitterFutureConversions extends TwitterFutureConversions

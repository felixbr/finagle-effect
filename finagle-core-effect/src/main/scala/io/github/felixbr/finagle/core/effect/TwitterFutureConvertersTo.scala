package io.github.felixbr.finagle.core.effect

import cats.effect.{Async, Bracket, ContextShift}
import com.twitter.util.Future

import scala.language.implicitConversions

trait TwitterFutureConvertersTo[F[_]] {

  /**
    * `futureToAsync` does not shift threadpools correctly, so we patch this here
    *
    * see: https://github.com/travisbrown/catbird/pull/209
    */
  def futureToF[A](fa: => Future[A])(implicit F: Async[F], CS: ContextShift[F]): F[A] = {
    import io.catbird.util.effect.futureToAsync

    Bracket[F, Throwable].guarantee { // think `finally`
      futureToAsync[F, A](fa)
    }(ContextShift[F].shift) // Shift back to the normal threadpool (usually from IOApp)
  }
}

/**
  * Same as `TwitterFutureConverters` but implicitly.
  *
  * If the implicit is not picked up, you can either use `futureToF` directly or provide a
  * type annotation to fix the implicit lookup.
  */
trait TwitterFutureConversionsTo[F[_]] extends TwitterFutureConvertersTo[F] {

  implicit def implicitFutureToF[A](fa: => Future[A])(implicit F: Async[F], CS: ContextShift[F]): F[A] =
    futureToF[A](fa)
}

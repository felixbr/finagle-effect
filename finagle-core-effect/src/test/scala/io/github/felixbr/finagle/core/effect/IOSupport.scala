package io.github.felixbr.finagle.core.effect

import java.util.concurrent.Executors

import cats.effect.{ContextShift, IO, Timer}

import scala.concurrent.ExecutionContext

trait IOSupport {

  val exectuionContext: ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())

  implicit val contextShift: ContextShift[IO] = IO.contextShift(exectuionContext)
  implicit val timer: Timer[IO]               = IO.timer(exectuionContext)
}

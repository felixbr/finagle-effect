package io.github.felixbr.finagle.core.effect

import java.util.concurrent.TimeUnit

import scala.concurrent.duration._
import scala.language.implicitConversions

trait TwitterDurationConversions {

  implicit def twitterDurationToScalaFiniteDuration(
    duration: com.twitter.util.Duration
  ): scala.concurrent.duration.FiniteDuration =
    duration.inUnit(TimeUnit.NANOSECONDS).nanos

  implicit def scalaFiniteDurationToTwitterDuration(
    duration: scala.concurrent.duration.FiniteDuration
  ): com.twitter.util.Duration =
    com.twitter.util.Duration.fromNanoseconds(duration.toNanos)
}

object TwitterDurationConversions extends TwitterDurationConversions

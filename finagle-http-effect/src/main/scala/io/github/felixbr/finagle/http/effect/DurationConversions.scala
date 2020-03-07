package io.github.felixbr.finagle.http.effect

import java.util.concurrent.TimeUnit

import scala.concurrent.duration._
import scala.language.implicitConversions

trait DurationConversions {

  implicit def twitterDurationToScalaFiniteDuration(
    duration: com.twitter.util.Duration
  ): scala.concurrent.duration.FiniteDuration =
    duration.inUnit(TimeUnit.NANOSECONDS).nanos

  implicit def scalaFiniteDurationToTwitterDuration(
    duration: scala.concurrent.duration.FiniteDuration
  ): com.twitter.util.Duration =
    com.twitter.util.Duration.fromNanoseconds(duration.toNanos)
}

object DurationConversions extends DurationConversions

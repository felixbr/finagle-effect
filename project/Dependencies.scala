import sbt._

object Version {
  val finagle  = "20.3.0"
  val log4cats = "1.0.0"
  val logback  = "1.2.3"
}

object Dependencies {

  object finagle {
    val core   = "com.twitter" %% "finagle-core" % Version.finagle
    val http   = "com.twitter" %% "finagle-http" % Version.finagle
    val thrift = "com.twitter" %% "finagle-http" % Version.finagle
  }

  object catbird {
    val effect = "io.catbird" %% "catbird-effect" % Version.finagle
  }

  val scalaTest  = "org.scalatest"  %% "scalatest"  % "3.0.8"  % "test"
  val scalaCheck = "org.scalacheck" %% "scalacheck" % "1.14.3" % "test"

  object logging {
    val log4cats = "io.chrisdavenport" %% "log4cats-slf4j" % Version.log4cats
    val logback  = "ch.qos.logback"    % "logback-classic" % Version.logback

    val viaLogback = List(log4cats, logback)
  }
}

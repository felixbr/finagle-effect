import sbt._

object Version {
  val finagle  = "20.3.0" // when you change this, also change the scrooge version in plugins.sbt!
  val log4cats = "1.0.0"
  val logback  = "1.2.3"
}

object Dependencies {

  object finagle {
    val core   = "com.twitter" %% "finagle-core"   % Version.finagle
    val http   = "com.twitter" %% "finagle-http"   % Version.finagle
    val thrift = "com.twitter" %% "finagle-thrift" % Version.finagle
  }

  object catbird {
    val effect = "io.catbird" %% "catbird-effect" % Version.finagle
  }

  val scrooge = List(
    "com.twitter"       %% "scrooge-core"   % Version.finagle,
    "com.twitter"       %% "finagle-thrift" % Version.finagle,
    "org.apache.thrift" % "libthrift"       % "0.10.0"
  )

  val scalaTest  = "org.scalatest"  %% "scalatest"  % "3.0.8"  % "test"
  val scalaCheck = "org.scalacheck" %% "scalacheck" % "1.14.3" % "test"

  object logging {
    val log4cats         = "io.chrisdavenport" %% "log4cats-slf4j" % Version.log4cats
    val logback          = "ch.qos.logback"    % "logback-classic" % Version.logback
    val julToSlf4jBridge = "org.slf4j"         % "jul-to-slf4j"    % "1.7.28" // This also needs to be enabled in the code via `SLF4JBridgeHandler.install()`

    val viaLogback = List(log4cats, logback, julToSlf4jBridge)
  }
}

import sbt._

object Version {
  val finagle  = "20.1.0"
  val log4cats = "1.0.0"
  val logback  = "1.2.3"
}

object Dependencies {

  val finagleHttp   = "com.twitter" %% "finagle-http"   % Version.finagle
  val catbirdEffect = "io.catbird"  %% "catbird-effect" % Version.finagle

  val scalaTest  = "org.scalatest"  %% "scalatest"  % "3.0.8"  % "test"
  val scalaCheck = "org.scalacheck" %% "scalacheck" % "1.14.1" % "test"

  object logging {
    val log4cats = "io.chrisdavenport" %% "log4cats-slf4j" % Version.log4cats
    val logback  = "ch.qos.logback"    % "logback-classic" % Version.logback

    val viaLogback = List(log4cats, logback)
  }
}

Global / onChangedBuildSource := ReloadOnSourceChanges
ThisBuild / turbo := true

lazy val root = (project in file("."))
  .withId("finagle-effect")
  .settings(sharedSettings: _*)
  .settings(noPublishSettings: _*)
  .aggregate(finagleCoreEffect, finagleHttpEffect, finagleThriftEffect, examples)

lazy val finagleCoreEffect = (project in file("finagle-core-effect"))
  .withId("core")
  .settings(sharedSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    name := "finagle-core-effect",
    libraryDependencies ++= List(
      Dependencies.finagle.core,
      Dependencies.catbird.effect,
    ),
    libraryDependencies ++= Dependencies.logging.viaLogback.map(_ % Test)
  )

lazy val finagleHttpEffect = (project in file("finagle-http-effect"))
  .withId("http")
  .settings(sharedSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    name := "finagle-http-effect",
    libraryDependencies ++= List(
      Dependencies.finagle.http,
      Dependencies.catbird.effect,
    )
  )
  .dependsOn(finagleCoreEffect % "test->test;compile->compile")

lazy val finagleThriftEffect = (project in file("finagle-thrift-effect"))
  .withId("thrift")
  .settings(sharedSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    name := "finagle-thrift-effect",
    libraryDependencies ++= List(
      Dependencies.finagle.thrift,
      Dependencies.catbird.effect,
    )
  )
  .dependsOn(finagleCoreEffect % "test->test;compile->compile", generatedThriftService % "test->test")

lazy val examples = (project in file("examples"))
  .settings(sharedSettings: _*)
  .settings(noPublishSettings: _*)
  .settings(
    libraryDependencies ++= List(
      Dependencies.finagle.http,
      Dependencies.finagle.thrift,
      Dependencies.catbird.effect,
      Dependencies.monix,
    ),
    libraryDependencies ++= Dependencies.logging.viaLogback,
    // improved for-comprehensions
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
    scalacOptions -= "-Xfatal-warnings"
  )
  .dependsOn(finagleHttpEffect, finagleThriftEffect, generatedThriftService)

lazy val generatedThriftService = (project in file("generated-thrift-service"))
  .settings(sharedSettings: _*)
  .settings(noPublishSettings: _*)
  .settings(
    libraryDependencies ++= Dependencies.scrooge,
    scalacOptions += "-deprecation:false" // Needed for generated code
  )

lazy val sharedSettings: List[Def.SettingsDefinition] = List(
  scalaVersion := "2.13.1",
  crossScalaVersions := List("2.12.10", "2.13.1"),
  libraryDependencies ++= List(
    Dependencies.scalaTest,
    Dependencies.scalaCheck,
  ),
  // format: off
  scalacOptions ++= List( // useful compiler flags for scala
    "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
    "-encoding", "utf-8",                // Specify character encoding used by source files.
    "-explaintypes",                     // Explain type errors in more detail.
    "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.
    "-language:existentials",            // Existential types (besides wildcard types) can be written and inferred
    "-language:experimental.macros",     // Allow macro definition (besides implementation and application)
    "-language:higherKinds",             // Allow higher-kinded types
    "-language:implicitConversions",     // Allow definition of implicit functions called views
    "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
    "-Xcheckinit",                       // Wrap field accessors to throw an exception on uninitialized access.
    "-Xfatal-warnings",                  // Fail the compilation if there are any warnings.
    "-Xlint:adapted-args",               // Warn if an argument list is modified to match the receiver.
    "-Xlint:constant",                   // Evaluation of a constant arithmetic expression results in an error.
    "-Xlint:delayedinit-select",         // Selecting member of DelayedInit.
    "-Xlint:doc-detached",               // A Scaladoc comment appears to be detached from its element.
    "-Xlint:inaccessible",               // Warn about inaccessible types in method signatures.
    "-Xlint:infer-any",                  // Warn when a type argument is inferred to be `Any`.
    "-Xlint:missing-interpolator",       // A string literal appears to be missing an interpolator id.
    "-Xlint:nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Xlint:nullary-unit",               // Warn when nullary methods return Unit.
    "-Xlint:option-implicit",            // Option.apply used implicit view.
    "-Xlint:package-object-classes",     // Class or object defined in package object.
    "-Xlint:poly-implicit-overload",     // Parameterized overloaded implicit methods are not visible as view bounds.
    "-Xlint:private-shadow",             // A private field (or class parameter) shadows a superclass field.
    "-Xlint:stars-align",                // Pattern sequence wildcard must align with sequence component.
    "-Xlint:type-parameter-shadow",      // A local type parameter shadows a type already in scope.
    "-Ywarn-dead-code",                  // Warn when dead code is identified.
    "-Ywarn-extra-implicit",             // Warn when more than one implicit parameter section is defined.
    "-Ywarn-numeric-widen",              // Warn when numerics are widened.
    "-Ywarn-unused:implicits",           // Warn if an implicit parameter is unused.
    "-Ywarn-unused:patvars",             // Warn if a variable bound in a pattern is unused.
    "-Ywarn-value-discard"               // Warn when non-Unit expression results are unused.
  ),
  // format: on
  // settings for sbt-release
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  releaseVcsSign := true,
)

lazy val publishSettings = List(
  organization := "io.github.felixbr",
  homepage := Some(url("https://github.com/felixbr/finagle-effect")),
  licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ =>
    false
  },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots".at(nexus + "content/repositories/snapshots"))
    else
      Some("releases".at(nexus + "service/local/staging/deploy/maven2"))
  },
  autoAPIMappings := true,
  apiURL := None,
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/felixbr/finagle-effect"),
      "scm:git:git@github.com:felixbr/finagle-effect.git"
    )
  ),
  pomExtra :=
    <developers>
      <developer>
        <id>felixbr</id>
        <name>Felix Bruckmeier</name>
        <url>https://github.com/felixbr</url>
      </developer>
    </developers>
)

lazy val noPublishSettings = List(
  skip in publish := true
)

addCommandAlias("scalafmtFormatAll", "; finagle-effect/scalafmtAll; finagle-effect/scalafmtSbt")
addCommandAlias("scalafmtValidateAll", "; finagle-effect/scalafmtCheckAll; finagle-effect/scalafmtSbtCheck")

addCommandAlias("validate", "; +finagle-effect/test:compile; scalafmtValidateAll; +finagle-effect/test")

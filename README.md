# finagle-effect

![Scala CI](https://github.com/felixbr/finagle-effect/workflows/Scala%20CI/badge.svg)

Wrappers around finagle clients (e.g. `finagle-http` and `finagle-thrift`), so they can easily be used 
in applications written in terms of `cats-effect` (e.g. `cats.effect.IO` or `monix.eval.Task`).

## Versions

`finagle-effect` depends on [`catbird`](https://github.com/travisbrown/catbird) which pins the versions of [`finagle`](https://github.com/twitter/finagle) libraries and [`cats-effect`](https://github.com/typelevel/cats-effect).

| finagle-effect | Twitter OSS | cats-effect |
|----------------|-------------|-------------|
| 0.3.0          | 20.3.0      | 2.0.0       |

## finagle-http-effect

```sbt
libraryDependencies += "io.github.felixbr" %% "finagle-http-effect" % <version>
```

### Usage Example

```scala
import cats.effect._
import com.twitter.finagle.http.Request
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.github.felixbr.finagle.core.effect.TwitterDurationConversions
import io.github.felixbr.finagle.http.effect.client._

import scala.concurrent.duration._

object ExampleIOApp extends IOApp with TwitterDurationConversions {
  implicit def log: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] =
    FinagleHttpClientBuilder[IO]
      .withUpdatedConfig(_.withRequestTimeout(5.seconds))
      .serviceResource("google.com:80")
      .use { googleClient =>
        for {
          _   <- log.info(s"Sending request")
          res <- googleClient(Request())
          _   <- log.info(s"Received: ${res.contentString.take(200)}")
          _   <- log.info("Done")
        } yield ExitCode.Success
      }
}
```

## finagle-thrift-effect

```sbt
libraryDependencies += "io.github.felixbr" %% "finagle-thrift-effect" % <version>
```

### Usage Example

```scala
import cats.effect._
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.github.felixbr.finagle.core.effect.{TwitterDurationConversions, TwitterFutureConversionsTo}
import io.github.felixbr.finagle.thrift.effect.client.FinagleThriftClientBuilder
import thrift.EchoService

import scala.concurrent.duration._

/**
  * `TwitterFutureConversionsTo[IO]` will try to convert `Future[A]` implicitly to `IO[A]` but in some cases the
  * type-inference isn't good enough, so you manually have to use `futureToF` to wrap calls that return `Future[A]`.
  *
  * Alternatively you can also help the type-inference by adding a type-annotation like
  * `echoService.echo("Hi"): IO[String]`.
  */
object ExampleThriftIOApp extends IOApp with TwitterDurationConversions with TwitterFutureConversionsTo[IO] {
  implicit def log: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] =
    demoThriftServerResource.use { port => // Just so we have a server to call to in this example

      FinagleThriftClientBuilder[IO]
        .withUpdatedConfig(_.withRequestTimeout(5.seconds))
        .serviceResource[thrift.EchoService.MethodPerEndpoint](s"localhost:$port")
        .use { echoClient =>
          for {
            _   <- log.info(s"Sending request")
            res <- futureToF(echoClient.echo("Hi!")) // Here the type-inference is not clear enough, so you have to use `futureToF` or add `: IO[String]`
            _   <- log.info(s"Received: $res")
            _   <- log.info("Done")
          } yield ExitCode.Success
        }
    }

  /**
    * For demonstration purposes
    */
  private def demoThriftServerResource: Resource[IO, Int] = {
    import com.twitter.finagle.Thrift
    import com.twitter.util.Future

    val port = 12345

    Resource
      .make {
        IO {
          Thrift.server.serveIface(s":$port", new EchoService.MethodPerEndpoint {
            override def echo(input: String): Future[String] = Future.value(input)
          })
        }
      }(server => server.close()) // Here the type-inference works, so you can call `.close()` as if it returned `IO`
      .map(_.boundAddress.asInstanceOf[java.net.InetSocketAddress].getPort)
  }
}
```
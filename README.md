# finagle-effect

![Scala CI](https://github.com/felixbr/finagle-effect/workflows/Scala%20CI/badge.svg)

Wrappers around finagle clients (e.g. `finagle-http` and `finagle-thrift`), so they can easily be used 
in applications written in terms of `cats-effect` (e.g. `cats.effect.IO` or `monix.eval.Task`).

## Versions

`finagle-effect` depends on [`catbird`](https://github.com/travisbrown/catbird) which pins the versions of [`finagle`](https://github.com/twitter/finagle) libraries and [`cats-effect`](https://github.com/typelevel/cats-effect).

| finagle-effect | Twitter OSS | cats-effect |
|----------------|-------------|-------------|
| 0.2.1          | 20.3.0      | 2.0.0       |

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
import com.twitter.util.Future
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.github.felixbr.finagle.core.effect.{TwitterDurationConversions, TwitterFutureConversions}
import io.github.felixbr.finagle.thrift.effect.client.FinagleThriftClientBuilder
import thrift.EchoService

import scala.concurrent.duration._

object ExampleThriftIOApp extends IOApp with TwitterDurationConversions with TwitterFutureConversions {
  implicit def log: Logger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] =
    demoThriftServerResource.use { port => // Just so we have a server to call to in this example

      FinagleThriftClientBuilder[IO]
        .withUpdatedConfig(_.withRequestTimeout(5.seconds))
        .serviceResource[thrift.EchoService[Future]](s"localhost:$port")
        .use { echoClient =>
          for {
            _   <- log.info(s"Sending request")
            res <- futureToIO(echoClient.echo("Hi!"))
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

    val port = 12345

    Resource
      .make {
        IO {
          Thrift.server.serveIface(s":$port", new EchoService[Future] {
            override def echo(input: String): Future[String] = Future.value(input)
          })
        }
      }(server => futureToIO(server.close()))
      .map(_.boundAddress.asInstanceOf[java.net.InetSocketAddress].getPort)
  }
}
```
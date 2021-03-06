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

import cats.effect._
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
        .serviceResource[thrift.EchoService.MethodPerEndpoint](s"localhost:$port")
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
    import com.twitter.util.Future

    val port = 12345

    Resource
      .make {
        IO {
          Thrift.server.serveIface(s":$port", new EchoService.MethodPerEndpoint {
            override def echo(input: String): Future[String] = Future.value(input)
          })
        }
      }(server => futureToIO(server.close()))
      .map(_.boundAddress.asInstanceOf[java.net.InetSocketAddress].getPort)
  }
}

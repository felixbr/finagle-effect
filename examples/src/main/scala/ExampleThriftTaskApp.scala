import cats.effect._
import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.github.felixbr.finagle.core.effect.{TwitterDurationConversions, TwitterFutureConversionsTo}
import io.github.felixbr.finagle.thrift.effect.client.FinagleThriftClientBuilder
import monix.eval.{Task, TaskApp}
import thrift.EchoService

import scala.concurrent.duration._

/**
  * You should notice that we can simply replace `IO` with `monix.eval.Task` and everything works the same as in
  * `ExampleThriftTaskApp`.
  */
object ExampleThriftTaskApp extends TaskApp with TwitterDurationConversions with TwitterFutureConversionsTo[Task] {
  implicit def log: Logger[Task] = Slf4jLogger.getLogger[Task]

  override def run(args: List[String]): Task[ExitCode] =
    demoThriftServerResource.use { port => // Just so we have a server to call to in this example

      FinagleThriftClientBuilder[Task]
        .withUpdatedConfig(_.withRequestTimeout(5.seconds))
        .serviceResource[thrift.EchoService.MethodPerEndpoint](s"localhost:$port")
        .use { echoClient =>
          for {
            _   <- log.info(s"Sending request")
            res <- futureToF(echoClient.echo("Hi!"))
            _   <- log.info(s"Received: $res")
            _   <- log.info("Done")
          } yield ExitCode.Success
        }
    }

  /**
    * For demonstration purposes
    */
  private def demoThriftServerResource: Resource[Task, Int] = {
    import com.twitter.finagle.Thrift
    import com.twitter.util.Future

    val port = 12345

    Resource
      .make {
        Task {
          Thrift.server.serveIface(s":$port", new EchoService.MethodPerEndpoint {
            override def echo(input: String): Future[String] = Future.value(input)
          })
        }
      }(server => server.close())
      .map(_.boundAddress.asInstanceOf[java.net.InetSocketAddress].getPort)
  }
}

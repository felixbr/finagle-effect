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

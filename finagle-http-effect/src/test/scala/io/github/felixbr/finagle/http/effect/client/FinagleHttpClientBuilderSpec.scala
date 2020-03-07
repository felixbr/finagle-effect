package io.github.felixbr.finagle.http.effect.client

import java.net.InetSocketAddress

import cats.effect._
import com.twitter.finagle.{Http, Service}
import com.twitter.finagle.http._
import com.twitter.util.{Await, Future}
import io.github.felixbr.finagle.http.effect.{DurationConversions, IOSupport}
import org.scalatest._

import scala.concurrent.duration._

class FinagleHttpClientBuilderSpec extends WordSpec with IOSupport with DurationConversions {

  def withServer[R](f: String => R): R = {
    val service = new Service[Request, Response] {
      def apply(req: Request): Future[Response] = {
        val response = Response(req.version, Status.Ok)
        response.contentString = "my content"

        Future.value(response)
      }
    }
    val server  = Http.serve(":0", service)
    val address = server.boundAddress.asInstanceOf[InetSocketAddress]

    val result = f(s"localhost:${address.getPort}")

    Await.ready(server.close())
    result
  }

  ".serviceResource" must {
    "provide a working client" in {
      withServer { dest =>
        val response =
          FinagleHttpClientBuilder[IO]
            .withUpdatedConfig(_.withRequestTimeout(5.seconds))
            .serviceResource(dest)
            .use { httpClient =>
              httpClient(Request()).map(identity)
            }
            .unsafeRunSync()

        assert(response.contentString == "my content")
        assert(response.status == Status.Ok)
      }
    }
  }
}

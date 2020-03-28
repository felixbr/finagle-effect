package io.github.felixbr.finagle.http.effect.client

import cats.effect._
import com.twitter.finagle.http._
import io.github.felixbr.finagle.core.effect._
import org.scalatest._

import scala.concurrent.duration._

class FinagleHttpClientBuilderSpec
    extends WordSpec
    with IOSupport
    with HttpTestServerSupport
    with TwitterDurationConversions
    with FinagleLoggingSetup {

  ".serviceResource" must {
    "provide a working client" in {
      val (receivedRequests, response) = withTestServer { server =>
        FinagleHttpClientBuilder[IO]
          .withUpdatedConfig(_.withRequestTimeout(5.seconds))
          .serviceResource(server.address, server.label)
          .use { httpClient =>
            httpClient(Request())
          }
          .unsafeRunSync()
      }

      assert(receivedRequests.size == 1)

      assert(response.contentString == TestServer.defaultResponse.contentString)
      assert(response.status == TestServer.defaultResponse.status)
    }
  }
}

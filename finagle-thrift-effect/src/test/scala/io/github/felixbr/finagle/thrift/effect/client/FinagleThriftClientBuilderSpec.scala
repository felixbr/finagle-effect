package io.github.felixbr.finagle.thrift.effect.client

import cats.effect._
import com.twitter.util.Future
import io.github.felixbr.finagle.core.effect.{IOSupport, TwitterDurationConversions, TwitterFutureConversions}
import org.scalatest._

import scala.concurrent.duration._

class FinagleThriftClientBuilderSpec
    extends WordSpec
    with IOSupport
    with ThriftTestServerSupport
    with TwitterDurationConversions
    with TwitterFutureConversions {

  ".serviceResource" must {
    val msg = "foo"

    "provide a working client for IO" in {
      val (receivedRequests, response) = withTestServer { server =>
        FinagleThriftClientBuilder[IO]
          .withUpdatedConfig(_.withRequestTimeout(5.seconds))
          .serviceResource[thrift.EchoService[Future]](server.address, server.label)
          .use { thriftClient =>
            futureToIO(thriftClient.echo(msg))
          }
          .unsafeRunSync()
      }

      assert(receivedRequests == Vector(msg))
      assert(response == msg)
    }

    "provide a working client for F" in {
      class Fixture[F[_]: Async: ContextShift] {
        def client(server: TestServer): F[String] =
          FinagleThriftClientBuilder[F]
            .withUpdatedConfig(_.withRequestTimeout(5.seconds))
            .serviceResource[thrift.EchoService[Future]](server.address, server.label)
            .use { thriftClient =>
              thriftClient.echo(msg)
            }
      }

      val (receivedRequests, response) = withTestServer { server =>
        new Fixture[IO].client(server).unsafeRunSync()
      }

      assert(receivedRequests == Vector(msg))
      assert(response == msg)
    }
  }
}
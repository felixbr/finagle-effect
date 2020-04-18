package io.github.felixbr.finagle.thrift.effect.client

import cats.effect._
import com.twitter.finagle.ServiceClosedException
import io.github.felixbr.finagle.core.effect._
import org.scalatest._

import scala.concurrent.duration._

class FinagleThriftClientBuilderSpec
    extends WordSpec
    with IOSupport
    with ThriftTestServerSupport
    with TwitterDurationConversions
    with TwitterFutureConversionsTo[IO]
    with FinagleLoggingSetup {

  ".serviceResource" must {
    val msg = "foo"

    "provide a working client for IO" in {
      val (receivedRequests, response) = withTestServer { server =>
        FinagleThriftClientBuilder[IO]
          .withUpdatedConfig(_.withRequestTimeout(5.seconds))
          .serviceResource[thrift.EchoService.MethodPerEndpoint](server.address, server.label)
          .use { thriftClient =>
            thriftClient.echo(msg)
          }
          .unsafeRunSync()
      }

      assert(receivedRequests == Vector(msg))
      assert(response == msg)
    }

    "provide a working client for F" in {
      class Fixture[F[_]: Async: ContextShift] extends TwitterFutureConversionsTo[F] {
        def client(server: TestServer): F[String] =
          FinagleThriftClientBuilder[F]
            .withUpdatedConfig(_.withRequestTimeout(5.seconds))
            .serviceResource[thrift.EchoService.MethodPerEndpoint](server.address, server.label)
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

    "close the thrift-client after the resource is no longer used" in {
      withTestServer { server =>
        val closedClient =
          FinagleThriftClientBuilder[IO]
            .withUpdatedConfig(_.withRequestTimeout(5.seconds))
            .serviceResource[thrift.EchoService.MethodPerEndpoint](server.address, server.label)
            .use { thriftClient =>
              IO.pure(thriftClient) // We intentionally leak a reference to the client; Don't try this at home!
            }

        // The leaked client should be already closed
        intercept[ServiceClosedException] {
          closedClient
            .flatMap { client =>
              client.echo(msg)
            }
            .unsafeRunSync()
        }
      }
    }
  }
}

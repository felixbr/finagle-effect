package io.github.felixbr.finagle.thrift.effect.client

import java.net.InetSocketAddress

import thrift.EchoService
import com.twitter.finagle._
import com.twitter.util.{Await, Future}

trait ThriftTestServerSupport {

  case class TestServer(address: Name.Bound) {
    val label = "test-server"
  }

  def withTestServer[R](f: TestServer => R): (Vector[String], R) = {
    var receivedRequests = Vector.empty[String]

    val service = new EchoService.MethodPerEndpoint {
      override def echo(input: String): Future[String] = {
        receivedRequests = receivedRequests :+ input

        Future.value(input)
      }
    }

    val server     = Thrift.server.serveIface(":0", service)
    val address    = Address(server.boundAddress.asInstanceOf[InetSocketAddress])
    val boundName  = Name.bound(address)
    val testServer = TestServer(boundName)

    val result = f(testServer)
    Await.ready(server.close())
    receivedRequests -> result
  }
}

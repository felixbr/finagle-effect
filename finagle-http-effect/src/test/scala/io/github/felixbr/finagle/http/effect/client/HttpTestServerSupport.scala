package io.github.felixbr.finagle.http.effect.client

import java.net.InetSocketAddress

import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle._
import com.twitter.util.{Await, Future}

trait HttpTestServerSupport {

  case class TestServer(address: Name.Bound) {
    val label = "test-server"
  }

  object TestServer {

    lazy val defaultResponse: Response = {
      val res = Response(Status.Ok)
      res.contentString = "test content"
      res
    }
  }

  def withTestServer[R](res: Response)(f: TestServer => R): (Vector[Request], R) = {
    var receivedRequests = Vector.empty[Request]

    val service = new Service[Request, Response] {
      def apply(req: Request): Future[Response] = {
        receivedRequests = receivedRequests :+ req

        Future.value(res)
      }
    }

    val server     = Http.serve(":0", service)
    val address    = Address(server.boundAddress.asInstanceOf[InetSocketAddress])
    val boundName  = Name.bound(address)
    val testServer = TestServer(boundName)

    val result = f(testServer)
    Await.ready(server.close())
    receivedRequests -> result
  }

  def withTestServer[R](f: TestServer => R): (Vector[Request], R) =
    withTestServer(TestServer.defaultResponse)(f)
}

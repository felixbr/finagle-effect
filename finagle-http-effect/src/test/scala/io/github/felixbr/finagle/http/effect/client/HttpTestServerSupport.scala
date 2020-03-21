package io.github.felixbr.finagle.http.effect.client

import java.net.InetSocketAddress

import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle._
import com.twitter.util.{Await, Future}

trait HttpTestServerSupport {

  case class TestServer(address: Name.Bound, receivedRequests: Vector[Request]) {
    val label = "test-server"
  }

  object TestServer {

    lazy val defaultResponse: Response = {
      val res = Response(Status.Ok)
      res.contentString = "test content"
      res
    }
  }

  def withTestServer[R](res: Response)(f: TestServer => R): R = {
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
    val testServer = TestServer(boundName, receivedRequests)

    val result = f(testServer)
    Await.ready(server.close())
    result
  }

  def withTestServer[R](f: TestServer => R): R =
    withTestServer(TestServer.defaultResponse)(f)
}

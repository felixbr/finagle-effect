package io.github.felixbr.finagle.thrift.effect.client

import cats.effect._
import cats.syntax.functor._
import com.twitter.finagle.Thrift.Client
import com.twitter.finagle.thrift.ThriftClientRequest
import com.twitter.finagle.{Name, Service, Thrift}
import com.twitter.util.Closable
import io.github.felixbr.finagle.core.effect._

import scala.concurrent.duration._
import scala.language.reflectiveCalls
import scala.reflect.ClassTag

object FinagleThriftClientBuilder {

  def apply[F[_]: Sync: Async: ContextShift] =
    new FinagleThriftClientBuilder[F](Sync[F].delay(Thrift.client), closeTimeout = 1.second)
}

final private[thrift] case class FinagleThriftClientBuilder[F[_]: Sync: Async: ContextShift](
  private val underlyingBuilder: F[Client],
  private val closeTimeout: FiniteDuration
) extends TwitterDurationConversions
    with TwitterFutureConverters {

  type ToClosable = {
    def asClosable: Closable
  }

  def withUpdatedConfig(f: Client => Client): FinagleThriftClientBuilder[F] =
    copy(underlyingBuilder = underlyingBuilder.map(f))

  def withCloseTimeout(duration: FiniteDuration): FinagleThriftClientBuilder[F] =
    copy(closeTimeout = duration)

  /* Highlevel API (using Scrooge) */

  def serviceResource[Srv <: ToClosable: ClassTag](
    dest: String,
  ): Resource[F, Srv] =
    makeServiceResource(
      underlyingBuilder.map(_.build[Srv](dest)),
      closeTimeout
    )

  def serviceResource[Srv <: ToClosable: ClassTag](
    dest: String,
    label: String
  ): Resource[F, Srv] =
    makeServiceResource(
      underlyingBuilder.map(_.build[Srv](dest, label)),
      closeTimeout
    )

  def serviceResource[Srv <: ToClosable: ClassTag](
    dest: Name,
    label: String
  ): Resource[F, Srv] =
    makeServiceResource(
      underlyingBuilder.map(_.build[Srv](dest, label)),
      closeTimeout
    )

  private def makeServiceResource[Srv <: ToClosable: ClassTag](
    newService: F[Srv],
    closeTimeout: FiniteDuration
  ): Resource[F, Srv] =
    Resource.make(newService)(service => futureToF(service.asClosable.close(closeTimeout)))

  /* Lowlevel API (you should probably use Scrooge instead) */

  def lowlevelServiceResource(
    dest: String,
  ): Resource[F, WrappedService[F, ThriftClientRequest, Array[Byte]]] =
    makeLowlevelServiceResource(
      underlyingBuilder.map(_.newService(dest)),
      closeTimeout
    )

  def lowlevelServiceResource(
    dest: String,
    label: String,
  ): Resource[F, WrappedService[F, ThriftClientRequest, Array[Byte]]] =
    makeLowlevelServiceResource(
      underlyingBuilder.map(_.newService(dest, label)),
      closeTimeout
    )

  def lowlevelServiceResource(
    dest: Name,
    label: String,
  ): Resource[F, WrappedService[F, ThriftClientRequest, Array[Byte]]] =
    makeLowlevelServiceResource(
      underlyingBuilder.map(_.newService(dest, label)),
      closeTimeout
    )

  private def makeLowlevelServiceResource(
    newService: F[Service[ThriftClientRequest, Array[Byte]]],
    closeTimeout: FiniteDuration
  ): Resource[F, WrappedService[F, ThriftClientRequest, Array[Byte]]] =
    Resource.make {
      newService.map(new WrappedService[F, ThriftClientRequest, Array[Byte]](_))
    }(service => service.close(closeTimeout))
}

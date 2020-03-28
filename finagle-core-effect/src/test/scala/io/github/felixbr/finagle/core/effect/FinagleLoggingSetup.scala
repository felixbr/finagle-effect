package io.github.felixbr.finagle.core.effect

import org.scalatest.{BeforeAndAfterAll, Suite}
import org.slf4j.bridge.SLF4JBridgeHandler

trait FinagleLoggingSetup extends BeforeAndAfterAll { self: Suite =>

  override protected def beforeAll(): Unit = {
    // redirect finagle logging (which uses java.util.logging) to SLF4J via jul-to-slf4j
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()

    super.beforeAll()
  }
}

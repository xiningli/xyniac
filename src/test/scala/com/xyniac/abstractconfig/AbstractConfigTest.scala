package com.xyniac.abstractconfig

import java.util.concurrent.atomic.AtomicBoolean

import com.xyniac.hotdeployment.HotDeployment
import org.scalatest.FunSuite
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.reflect.runtime.currentMirror
import scala.util.{Failure, Success, Try}

object WrongConfig extends AbstractConfig{
  println("preparing to break the initialization")
  throw new RuntimeException()
}

object ResultHolder{
  val result = new AtomicBoolean(false)
}

class AbstractConfigTest extends FunSuite {
  test("Correctly read the name Xining") { // Uses ScalaTest assertions
    assert(TestAbstractConfig.getName()=="Xining")
  }

  test("test error config initialization handling") {
    TestAbstractConfig.getSupervisor()
    try {
      WrongConfig
    } catch {
      case err: Error => println("manually produced error")
      case e: Exception => println("manually produced Exception")
    }

    Thread.sleep(5000)

    HotDeployment.runScalaCommand("object WrongConfig extends com.xyniac.abstractconfig.AbstractConfig{com.xyniac.abstractconfig.ResultHolder.result.set(true)};WrongConfig")
    assert(ResultHolder.result.get())
  }

}

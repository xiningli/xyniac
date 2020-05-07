package com.xyniac.abstractconfig

import org.scalatest.FunSuite
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.reflect.runtime.currentMirror
import scala.util.{Failure, Success, Try}

object WrongConfig extends AbstractConfig{
  println("preparing to break the initialization")
  throw new RuntimeException()
}

class AbstractConfigTest extends FunSuite {
  test("Correctly read the name Mike") { // Uses ScalaTest assertions
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

    Thread.sleep(20000)
  }

}

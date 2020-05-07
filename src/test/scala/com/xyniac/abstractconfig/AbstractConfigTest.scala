package com.xyniac.abstractconfig

import org.scalatest.FunSuite
import org.json4s._
import org.json4s.native.JsonMethods._


class AbstractConfigTest extends FunSuite {
  test("Correctly read the name Mike") { // Uses ScalaTest assertions
    assert(TestAbstractConfig.getName()=="Xining")
  }

}

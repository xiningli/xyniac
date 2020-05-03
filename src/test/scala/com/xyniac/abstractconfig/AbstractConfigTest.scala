package com.xyniac.abstractconfig

import org.scalatest.FunSuite


class AbstractConfigTest extends FunSuite {
  test("Correctly read the name Mike") { // Uses ScalaTest assertions
    assert(TestAbstractConfig.getName()=="Xining")
  }
}

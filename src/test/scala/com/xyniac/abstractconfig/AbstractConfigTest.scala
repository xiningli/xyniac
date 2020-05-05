package com.xyniac.abstractconfig

import org.scalatest.FunSuite
import org.json4s._
import org.json4s.native.JsonMethods._


class AbstractConfigTest extends FunSuite {
  test("Correctly read the name Mike") { // Uses ScalaTest assertions
    assert(TestAbstractConfig.getName()=="Xining")
  }
  test ("test merging behavior") {
    val orig = "{\n  \"fileSystemFullyQualifiedName\": \"com.xyniac.abstractconfig.JavaInMemoryFileSystem\",\n  \"initialDelay\": 3000,\n  \"delay\": 10000\n}"
    val hotDeployed = "{}"
    val res = pretty(render(parse(orig) merge parse(hotDeployed)))
    println(res)
  }

  test ("test merging behavior2") {
    val orig = "{\n  \"fileSystemFullyQualifiedName\": \"com.xyniac.abstractconfig.JavaInMemoryFileSystem\",\n  \"initialDelay\": 3000,\n  \"delay\": 10000\n}"
    val hotDeployed = "{\n  \"fileSystemFullyQualifiedName\": \"com.xyniac.abstractconfig.InMemoryFileSystem\",\n  \"initialDelay\": 3000,\n  \"delay\": 10000\n}"
    val res = pretty(render(parse(orig) merge parse(hotDeployed)))
    println(res)
  }
}

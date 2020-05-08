package com.xyniac.abstractconfig

import java.nio.file.Files
import java.util.concurrent.atomic.AtomicBoolean

import com.xyniac.hotdeployment.HotDeployment
import org.scalatest.FunSuite
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.reflect.runtime.currentMirror
import scala.util.{Failure, Success, Try}

object WrongConfig extends AbstractConfig{
  println("preparing to break the initialization")
//  throw new RuntimeException()
}

object ResultHolder{
  val result = new AtomicBoolean(false)
}

class AbstractConfigTest extends FunSuite {
  test("Correctly read the name Xining") { // Uses ScalaTest assertions
    assert(TestAbstractConfig.getName()=="Xining")
  }

  test("test error config initialization handling") {
    println(TestAbstractConfig.getSupervisor())
    try {
      WrongConfig
    } catch {
      case err: Error => println("manually produced error")
      case e: Exception => println("manually produced Exception")
    }

    Thread.sleep(5000)
//    println("triggering hot deployment")
//    HotDeployment.runScalaCommand("object WrongConfig extends com.xyniac.abstractconfig.AbstractConfig{com.xyniac.abstractconfig.ResultHolder.result.set(true)};WrongConfig")
//    println("hot deployment triggered")
    val conf = AbstractConfig.confDirName
    val fs = new JavaInMemoryFileSystem
    Files.createDirectory(fs.getPath(conf))
    val name = TestAbstractConfig.getName()
    println(name)
    val inMemConfig =
      """
        |{
        |  "fileSystemFullyQualifiedName": "com.xyniac.abstractconfig.InMemoryFileSystem",
        |  "initialDelay": 3000,
        |  "delay": 10000
        |}
      """.stripMargin
    Files.write(fs.getPath(conf, "com.xyniac.abstractconfig.RemoteConfig$"), inMemConfig.getBytes())
    val fixedWrongConfig =
      """
        |{
        |  "flag": true,
        |}
      """.stripMargin
    Files.write(fs.getPath(conf, "com.xyniac.abstractconfig.WrongConfig$"), fixedWrongConfig.getBytes())
    println(AbstractConfig.checkAllConfig())
    Thread.sleep(20000)
    assert(ResultHolder.result.get())


  }

}

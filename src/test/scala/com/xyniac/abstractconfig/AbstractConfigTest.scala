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
  throw new RuntimeException()
  println("preparing to break the initialization22222")

}

object ResultHolder{
  val result = new AtomicBoolean(false)
}

class AbstractConfigTest extends FunSuite {
  test("Correctly read the name Xining") { // Uses ScalaTest assertions
    assert(TestAbstractConfig.getName()=="Xining")
  }

  test ("test half initialized config") {
    val conf = AbstractConfig.confDirName
    val fs = new JavaInMemoryFileSystem
    Files.createDirectory(fs.getPath(conf))
    val name = TestAbstractConfig.getName()
    println(name)
    val newTarget =
      """
        |{
        |  "fileSystemFullyQualifiedName": "com.xyniac.abstractconfig.InMemoryFileSystem",
        |  "initialDelay": 3000,
        |  "delay": 10000
        |}
      """.stripMargin

    Files.write(fs.getPath(conf, "com.xyniac.abstractconfig.RemoteConfig$"), newTarget.getBytes())
    println("written the new config")
    Thread.sleep(4000)
    val scalaFs = new InMemoryFileSystem
    Thread.sleep(4000)
    println(AbstractConfig.checkAllConfig())
    try {
      WrongConfig
    } catch {
      case err: Error => println("manually produced error")
      case e: Exception => println("manually produced Exception")
    }
    val fixedWrongConfig =
      """
        |{
        |  "flag": true,
        |}
      """.stripMargin
    Files.write(fs.getPath(conf, "com.xyniac.abstractconfig.WrongConfig$"), fixedWrongConfig.getBytes())
    println(AbstractConfig.checkAllConfig())
    println("getName: " + TestAbstractConfig.getName())
    Thread.sleep(20000)
    println(AbstractConfig.checkAllConfig())
  }

}

package com.xyniac.abstractconfig

import java.nio.file.{Files, Path, Paths}
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Stream

import com.xyniac.environment.Environment
import com.xyniac.abstractconfig.TestAbstractConfig

import scala.io.Source
import org.apache.commons.io.IOUtils
import org.scalatest.FunSuite

object InMemFsTest {
  val fileSystemTestSuccessFlag = new AtomicBoolean(false)
}

class InMemFsTest extends FunSuite {
  test("test correctly reload the file system") {
    val conf = "conf"
    val fs = new JavaInMemoryFileSystem
    val p = fs.getPath("p")

    Files.createDirectory(fs.getPath("test0"))
    Files.createDirectory(fs.getPath(conf))

    val configInResource = Source.fromResource(Paths.get(conf, RemoteConfig.getClass.getCanonicalName).toString)

    val targetArray = IOUtils.toByteArray(configInResource.reader())
    Files.write(fs.getPath(conf, RemoteConfig.getClass.getCanonicalName), targetArray)

    val ls = Files.list(fs.getPath(conf))
    val targetInputStreamFromInMem = Files.newInputStream(fs.getPath(conf, RemoteConfig.getClass.getCanonicalName))
    val targetArrayFromInMem = IOUtils.toByteArray(targetInputStreamFromInMem)
    ls.forEach(println(_))
    assert(new String(targetArray) == new String(targetArrayFromInMem))
    println(new String(targetArrayFromInMem))
    println(TestAbstractConfig.getConfigJson())

    val newTarget =
      """
        |{
        |  "fileSystemFullyQualifiedName": "com.xyniac.abstractconfig.InMemoryFileSystem",
        |  "initialDelay": 3000,
        |  "delay": 10000
        |}
      """.stripMargin


    Thread.sleep(20000)
    Files.write(fs.getPath(conf, RemoteConfig.getClass.getCanonicalName), newTarget.getBytes())
    Thread.sleep(20000)

    assert(InMemFsTest.fileSystemTestSuccessFlag.get())

  }
}

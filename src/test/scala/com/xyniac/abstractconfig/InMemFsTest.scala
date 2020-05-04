package com.xyniac.abstractconfig

import java.nio.file.{Files, Path, Paths}
import java.util.stream.Stream

import com.xyniac.environment.Environment

import scala.io.Source
import org.apache.commons.io.IOUtils
object InMemFsTest {
  def main(args: Array[String]): Unit = {
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
    println(new String(targetArrayFromInMem))

  }
}

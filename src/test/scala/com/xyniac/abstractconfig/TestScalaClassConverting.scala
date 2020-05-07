package com.xyniac.abstractconfig

import java.nio.file.{Files, Paths}

import com.google.gson.Gson
import com.xyniac.abstractconfig.TestAbstractConfig.Supervisor
import org.apache.commons.io.IOUtils
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods.{parse, pretty, render}
import org.scalatest.FunSuite

import reflect.runtime.currentMirror
import scala.io.Source
import java.util.concurrent.CopyOnWriteArrayList
class TestScalaClassConverting extends FunSuite {

  test("test correctly reload the reflection") {
    System.setProperty("iaas", "aws")
    System.setProperty("env", "dev")
    System.setProperty("region", "EU_NORTH_1")
    val module =currentMirror.staticModule("com.xyniac.abstractconfig.TestAbstractConfig$")
    val mirror = currentMirror.reflectModule(module)
    val config = mirror.instance.asInstanceOf[AbstractConfig]
    assert(config.getProperty("supervisor", classOf[Supervisor]).toString== "Supervisor(Bob,50)")
  }


}

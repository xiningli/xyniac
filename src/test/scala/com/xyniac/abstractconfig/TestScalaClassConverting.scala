package com.xyniac.abstractconfig

import java.nio.file.{Files, Paths}

import com.google.gson.Gson
import org.apache.commons.io.IOUtils
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods.{parse, pretty, render}
import org.scalatest.FunSuite

import scala.io.Source

class TestScalaClassConverting extends FunSuite {

  test("test correctly reload the file system") {
    System.setProperty("iaas", "aws")
    System.setProperty("env", "dev")
    System.setProperty("region", "EU_NORTH_1")
    println(TestAbstractConfig.getSupervisor().toString)
  }

}

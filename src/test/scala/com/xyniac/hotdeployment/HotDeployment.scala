package com.xyniac.hotdeployment

import scala.reflect.runtime.universe
import scala.tools.reflect.ToolBox

object HotDeployment {
  def runScalaCommand(sourceCode: String):Any = this.synchronized{
    val tb = universe.runtimeMirror(getClass.getClassLoader).mkToolBox()
    val command = sourceCode
    val parsed = tb.parse(command)
    val res = tb.eval(parsed)
    res
  }
}

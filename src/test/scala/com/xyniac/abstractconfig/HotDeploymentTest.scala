package com.xyniac.abstractconfig

import java.util.concurrent.atomic.AtomicBoolean

import com.xyniac.hotdeployment.HotDeployment
import org.scalatest.FunSuite



object HotDeploymentTest{
  val result = new AtomicBoolean(false)
}

class HotDeploymentTest extends FunSuite {

  test ("HotDeploymentTest") {
    val name = TestAbstractConfig.getName()
    println(AbstractDynamicConfig.checkAllConfig())
    println("getName: " + TestAbstractConfig.getName())
    Thread.sleep(10000)
    println(AbstractDynamicConfig.checkAllConfig())
    HotDeployment.runScalaCommand(
      """
        |object HotDeploymentTestConfig extends com.xyniac.abstractconfig.AbstractDynamicConfig {
        | override def getKey: String = "com.xyniac.abstractconfig.HotDeploymentTestConfig$"
        |}
        |HotDeploymentTestConfig
      """.stripMargin)
    Thread.sleep(10000)
    assert(AbstractDynamicConfig.checkAllConfig().get("com.xyniac.abstractconfig.HotDeploymentTestConfig$").getAsJsonObject.get("flag").getAsBoolean)
  }

}

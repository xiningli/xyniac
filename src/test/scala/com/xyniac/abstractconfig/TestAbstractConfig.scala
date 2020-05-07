package com.xyniac.abstractconfig

object TestAbstractConfig extends AbstractConfig {
  case class Supervisor(name: String, age: Int)
  val jMap = new java.util.HashMap[String, String]
  jMap.put("name", "Mike")
  def getName(): String = {
    getProperty("name", classOf[String])
  }

  def getSupervisor(): Supervisor = {
    getProperty("supervisor", classOf[Supervisor])
  }

  def getCity():String= getProperty("city", classOf[String], Option("Unknown"))

  def getTrival(): String = {
    jMap.get("name")
  }
  override def toString:String = "TestAbstractConfig"
}

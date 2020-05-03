package com.xyniac.abstractconfig

object TestAbstractConfig extends AbstractConfig {
  val myMap = Map("name"->"Mike")
  val jMap = new java.util.HashMap[String, String]
  jMap.put("name", "Mike")
  def getName(): String = {
    getProperty("name", classOf[String])
  }

  def getCity():String= getProperty("city", classOf[String], Option("Unknown"))

  def getTrival(): String = {
    jMap.get("name")
  }
}

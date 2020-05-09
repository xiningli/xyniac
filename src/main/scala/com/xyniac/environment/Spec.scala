package com.xyniac.environment


case object Spec {
  val environment: String = System.getProperty("environment")
  val iaas: String = System.getProperty("iaas")
  val region: String = System.getProperty("region")
  override def toString: String = s"Env[$environment, $iaas, $region]"
}
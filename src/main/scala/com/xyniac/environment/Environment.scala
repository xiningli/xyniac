package com.xyniac.environment


case object Environment {
  val env: String = System.getProperty("env")
  val iaas: String = System.getProperty("iaas")
  val region: String = System.getProperty("region")
  override def toString: String = s"Env[$env, $iaas, $region]"
}
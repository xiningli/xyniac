package com.xyniac.abstractconfig

import java.nio.file.FileSystem
import java.util.concurrent.ScheduledFuture

import org.slf4j.LoggerFactory

object RemoteConfig extends AbstractConfig {
  private val logger = LoggerFactory.getLogger(this.getClass)
  val trigger: ScheduledFuture[String] = AbstractConfig.handler
  logger.info(trigger.get)

  def getInitialDelay(): Long = {
    getProperty("initialDelay", classOf[Long])
  }

  def getDelay(): Long = {
    getProperty("delay", classOf[Long])
  }

  def getFileSystem(): FileSystem = {
    val fileSystemFullyQualifiedName = getProperty("fileSystemFullyQualifiedName", classOf[String])
    val clazz = Class.forName(fileSystemFullyQualifiedName)
    clazz.newInstance().asInstanceOf[FileSystem]
  }
}

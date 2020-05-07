package com.xyniac.abstractconfig

import java.nio.file.FileSystem
import java.util.concurrent.ScheduledFuture

import org.slf4j.LoggerFactory

object RemoteConfig extends AbstractConfig {

  def getInitialDelay(): Long = {
    getProperty("initialDelay", classOf[Long])
  }

  def getDelay(): Long = {
    getProperty("delay", classOf[Long])
  }

  def getFileSystem(): FileSystem = {
    val fileSystemFullyQualifiedName = getProperty("fileSystemFullyQualifiedName", classOf[String])
    println(fileSystemFullyQualifiedName)
    val clazz = Class.forName(fileSystemFullyQualifiedName)
    clazz.newInstance().asInstanceOf[FileSystem]
  }
}

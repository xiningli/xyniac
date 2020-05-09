package com.xyniac.abstractconfig

import java.nio.file.FileSystem

object RemoteConfig extends AbstractDynamicConfig {

  def getInitialDelay: Long = {
    getProperty("initialDelay", classOf[Long])
  }

  def getDelay: Long = {
    getProperty("delay", classOf[Long])
  }

  def getFileSystem: FileSystem = {
    val fileSystemFullyQualifiedName = getProperty("fileSystemFullyQualifiedName", classOf[String])
    val clazz = Class.forName(fileSystemFullyQualifiedName)
    clazz.newInstance().asInstanceOf[FileSystem]
  }
}

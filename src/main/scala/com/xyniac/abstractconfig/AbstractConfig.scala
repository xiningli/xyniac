package com.xyniac.abstractconfig

import java.io.File
import java.nio.file.Paths
import java.util.concurrent.locks.{ReadWriteLock, ReentrantReadWriteLock}

import com.google.gson.{Gson, JsonObject}
import scala.reflect.runtime.currentMirror

import scala.jdk.CollectionConverters._
import scala.collection.concurrent.Map
import scala.io.Source
import com.xyniac.environment.Environment
import com.xyniac.tool.GsonTools
import com.xyniac.tool.GsonTools.ConflictStrategy
import org.reflections.Reflections
import java.util

import com.xyniac.abstractconfig.AbstractConfig.getClass

import scala.reflect.runtime.universe
import scala.util.{Failure, Success, Try}
abstract class AbstractConfig {
  private val lock: ReadWriteLock = new ReentrantReadWriteLock
  private val jsonFileName = this.getClass.getName
  private val confDirName = "conf"

  // original Json Object to fall back to
//  private val defaultConfig = AbstractConfigUtil.GSON.fromJson(Source.fromResource(jsonFileName).mkString, classOf[JsonObject])

  private val coldDeployedConfig = if (Environment.iaas!=null && Environment.region!=null && Environment.env!=null) {
    val defaultConfig = Try(AbstractConfigUtil.GSON.fromJson(Source.fromResource(Paths.get(confDirName, jsonFileName).toString).mkString, classOf[JsonObject])) match {
      case Success(s)=>s
      case Failure(e)=> new JsonObject
    }
    val envConfig = Try(AbstractConfigUtil.GSON.fromJson(Source.fromResource(Paths.get(confDirName, jsonFileName, Environment.env).toString).mkString, classOf[JsonObject])) match {
      case Success(s)=>s
      case Failure(e)=> new JsonObject
    }

    val iaasConfig =  Try(AbstractConfigUtil.GSON.fromJson(Source.fromResource(Paths.get(confDirName, jsonFileName, Environment.env, Environment.iaas).toString).mkString, classOf[JsonObject])) match {
      case Success(s)=>s
      case Failure(e)=> new JsonObject
    }
    val regionConfig =  Try(AbstractConfigUtil.GSON.fromJson(Source.fromResource(Paths.get(confDirName, Environment.env, Environment.iaas, Environment.region, jsonFileName).toString).mkString, classOf[JsonObject])) match {
      case Success(s)=>s
      case Failure(e)=> new JsonObject
    }
    val resultConfig = defaultConfig.deepCopy()
    GsonTools.extendJsonObject(resultConfig, ConflictStrategy.PREFER_SECOND_OBJ, java.util.Arrays.asList(envConfig, iaasConfig, regionConfig))
    resultConfig
  } else {
    AbstractConfigUtil.GSON.fromJson(Source.fromResource(Paths.get(confDirName, jsonFileName).toString).mkString, classOf[JsonObject])
  }
  private val hotDeployedConfig = new JsonObject()



  def setProperty[T](key: String, value: T): Unit = {
    lock.writeLock().lock()
    try {
      if (coldDeployedConfig.has(key)) {
        val jsonNode = coldDeployedConfig.get(key)
        try {
          AbstractConfigUtil.GSON.fromJson(jsonNode, value.getClass)
        } catch {
          case e: Exception => throw new IllegalArgumentException(s"the value of key $key in the code deploy config doesn't match the given type")
        }

      }
      hotDeployedConfig.add(key, AbstractConfigUtil.GSON.toJsonTree(value))
    } finally {
      lock.writeLock().unlock()
    }
  }

  def getProperty[T](key: String, classType: Class[T], defaultValue: Option[T] = Option.empty): T = {
    lock.readLock.lock()
    try {
      val jsonNode = if (hotDeployedConfig.has(key)) hotDeployedConfig.get(key) else coldDeployedConfig.get(key)
      val res = AbstractConfigUtil.GSON.fromJson(jsonNode, classType)
      res match {
        case null => throw new NullPointerException
        case _ => res
      }
    } catch {
      case _: Exception => defaultValue match {
        case None => throw new IllegalArgumentException(s"property $key is defined nowhere")
        case Some(t) => t
      }
    }finally {
      lock.readLock.unlock()
    }

  }

  def getConfigJson(): JsonObject = {
    val hotDeployedConfigCopy = {
      lock.readLock.lock()
      try {
        hotDeployedConfig.deepCopy()
      } finally {
        lock.readLock.unlock()
      }
    }
    val coldDeployedConfigCopy = {
      lock.readLock.lock()
      try {
        coldDeployedConfig.deepCopy()
      } finally {
        lock.readLock.unlock()
      }
    }
    val res = new JsonObject
    GsonTools.extendJsonObject(res, ConflictStrategy.PREFER_SECOND_OBJ, java.util.Arrays.asList(coldDeployedConfigCopy, hotDeployedConfigCopy))
    res
  }
}

object AbstractConfig {
  val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)

  def checkAllConfig(): JsonObject = {
    val reflections = new Reflections(getClass.getPackage)
    val classes = reflections.getSubTypesOf(classOf[AbstractConfig])
    val result = new JsonObject
    classes.parallelStream().forEach(clazz => {
      val canonicalName = clazz.getCanonicalName
      val module = runtimeMirror.staticModule(canonicalName)
      val obj = runtimeMirror.reflectModule(module)
      val currConfig:AbstractConfig = obj.instance.asInstanceOf[AbstractConfig]

      result.add(canonicalName, currConfig.getConfigJson())
    })
    result
  }
}



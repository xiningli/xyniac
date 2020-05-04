package com.xyniac.abstractconfig

import java.io.File
import java.nio.file.{Files, Paths}
import java.util.concurrent.locks.{ReadWriteLock, ReentrantReadWriteLock}

import com.google.gson.{Gson, JsonObject}

import scala.reflect.runtime.{currentMirror, universe}
import scala.jdk.CollectionConverters._
import scala.collection.concurrent.Map
import scala.io.Source
import com.xyniac.environment.Environment
import com.xyniac.tool.GsonTools
import com.xyniac.tool.GsonTools.ConflictStrategy
import org.reflections.Reflections
import java.util

import akka.actor.ActorSystem
import com.xyniac.abstractconfig.AbstractConfig.getClass
import org.json4s._
import org.json4s.native.JsonMethods._
import java.util.concurrent.{ScheduledFuture, TimeUnit}

import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration.{Duration, FiniteDuration}
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.stream.Collectors

import org.apache.commons.io.IOUtils

import scala.util.{Failure, Success, Try}
object AbstractConfig {
  val confDirName:String = "conf"

  private val registry = new java.util.concurrent.ConcurrentHashMap[String, AbstractConfig]

  private val logger = LoggerFactory.getLogger(this.getClass)
  private implicit val formats: DefaultFormats.type = DefaultFormats
  private val lock: ReadWriteLock = new ReentrantReadWriteLock
  private val gson: Gson = new Gson()

  private val task: Runnable = ()=>{
    try {
      val fileSystem = RemoteConfig.getFileSystem()
      logger.info("loading the config file system")
      logger.info(fileSystem.toString)
      registry.entrySet.parallelStream().forEach(entry => {
        val jsonFileName = entry.getKey

        val hotDeployedDefaultConfig:String = Try(IOUtils.toByteArray(Files.newInputStream(fileSystem.getPath(AbstractConfig.confDirName, jsonFileName)))) match {
          case Success(s)=>new String(s)
          case Failure(e)=> "{}"
        }
        val hotDeployedEnvConfig:String = Try(IOUtils.toByteArray(Files.newInputStream(fileSystem.getPath(AbstractConfig.confDirName, Environment.env, jsonFileName)))) match {
          case Success(s)=>new String(s)
          case Failure(e)=> "{}"
        }

        val hotDeployedIaasConfig:String = Try(IOUtils.toByteArray(Files.newInputStream(fileSystem.getPath(AbstractConfig.confDirName, Environment.env, Environment.iaas, jsonFileName)))) match {
          case Success(s)=>new String(s)
          case Failure(e)=> "{}"
        }

        val hotDeployedRegionConfig:String =  Try(IOUtils.toByteArray(Files.newInputStream(fileSystem.getPath(AbstractConfig.confDirName, Environment.env, Environment.iaas, Environment.region, jsonFileName)))) match {
          case Success(s)=>new String(s)
          case Failure(e)=> "{}"
        }

        val hotDeployedConfigJson4j = parse(hotDeployedDefaultConfig) merge parse(hotDeployedEnvConfig) merge parse(hotDeployedIaasConfig) merge parse(hotDeployedRegionConfig)
        val renderedHotDeployedConfigJson = pretty(render(hotDeployedConfigJson4j))

        val latestConfig = gson.fromJson(renderedHotDeployedConfigJson, classOf[JsonObject])
        logger.info(s"latestConfig for $jsonFileName:" + latestConfig)
        val currConfig = entry.getValue.getConfigJson()
        if (latestConfig==currConfig) {
          logger.info(s"config remains the same for $jsonFileName")
        } else {
          lock.writeLock().lock()
          try {
            entry.getValue.hotDeployedConfig = latestConfig
          } finally {
            lock.writeLock.unlock()
          }

        }
      }
      )


      logger.info(registry.toString)
    } catch {
      case e: Exception => {
        e.printStackTrace()
        logger.error(e.toString)
      }
    } finally {
      logger.info("refresh completed")
    }
  }

  private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor
  lazy val handle: ScheduledFuture[_] = scheduler.scheduleWithFixedDelay(task, RemoteConfig.getInitialDelay(), RemoteConfig.getDelay(), TimeUnit.MILLISECONDS)
  def checkAllConfig(): JsonObject = {

    val result = new JsonObject
    registry.entrySet().parallelStream().forEach(entry => result.add(entry.getKey, entry.getValue.getConfigJson()))
    result
  }
}


abstract class AbstractConfig {

  if (currentMirror.reflect(this).symbol.isModuleClass) {
    AbstractConfig.registry.put(this.getClass.getCanonicalName, this)
  } else {
    throw new IllegalStateException("AbstractConfig must be scala singleton")
  }

  val jsonFileName:String = this.getClass.getName

  val coldDeployedDefaultConfig:String = Try(Source.fromResource(Paths.get(AbstractConfig.confDirName, jsonFileName).toString).mkString) match {
    case Success(s)=>s
    case Failure(e)=> "{}"
  }
  val coldDeployedEnvConfig:String = Try(Source.fromResource(Paths.get(AbstractConfig.confDirName, Environment.env, jsonFileName).toString).mkString) match {
    case Success(s)=>s
    case Failure(e)=> "{}"
  }

  val coldDeployedIaasConfig:String =  Try(Source.fromResource(Paths.get(AbstractConfig.confDirName, Environment.env, Environment.iaas,  jsonFileName).toString).mkString) match {
    case Success(s)=>s
    case Failure(e)=> "{}"
  }

  val coldDeployedRegionConfig:String =  Try(Source.fromResource(Paths.get(AbstractConfig.confDirName, Environment.env, Environment.iaas, Environment.region, jsonFileName).toString).mkString) match {
    case Success(s)=>s
    case Failure(e)=> "{}"
  }



  private val coldDeployedConfigJson4j = parse(coldDeployedDefaultConfig) merge parse(coldDeployedEnvConfig) merge parse(coldDeployedIaasConfig) merge parse(coldDeployedRegionConfig)
  private val renderedConfigJson = pretty(render(coldDeployedConfigJson4j))

  private val coldDeployedConfig = AbstractConfig.gson.fromJson(renderedConfigJson, classOf[JsonObject])
  private var hotDeployedConfig = new JsonObject()



  def setProperty[T](key: String, value: T): Unit = {
    AbstractConfig.lock.writeLock().lock()
    try {
      if (coldDeployedConfig.has(key)) {
        val jsonNode = coldDeployedConfig.get(key)
        try {
          AbstractConfig.gson.fromJson(jsonNode, value.getClass)
        } catch {

          case e: Exception => throw new IllegalArgumentException(s"the value of key $key in the code deploy config cannot be converted the given type")
        }

      }
      hotDeployedConfig.add(key, AbstractConfig.gson.toJsonTree(value))
    } finally {
      AbstractConfig.lock.writeLock().unlock()
    }
  }

  def getProperty[T](key: String, classType: Class[T], defaultValue: Option[T] = Option.empty): T = {
    AbstractConfig.lock.readLock.lock()
    try {
      val jsonNode = if (hotDeployedConfig.has(key)) hotDeployedConfig.get(key) else coldDeployedConfig.get(key)
      val res = AbstractConfig.gson.fromJson(jsonNode, classType)
      res match {// TODO: add scala class fallback logic
        case null => throw new NullPointerException
        case _ => res
      }
    } catch {
      case _: Exception => defaultValue match {
        case None => throw new IllegalArgumentException(s"property $key is defined nowhere")
        case Some(t) => t
      }
    }finally {
      AbstractConfig.lock.readLock.unlock()
    }

  }

  def getConfigJson(): JsonObject = {
    val hotDeployedConfigCopy = {
      AbstractConfig.lock.readLock.lock()
      try {
        hotDeployedConfig.toString
      } finally {
        AbstractConfig.lock.readLock.unlock()
      }
    }
    val coldDeployedConfigCopy = coldDeployedConfig.toString
    val res = parse(coldDeployedConfigCopy) merge parse(hotDeployedConfigCopy)
    AbstractConfig.gson.fromJson(pretty(render(res)), classOf[JsonObject])
  }
}



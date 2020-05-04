package com.xyniac.abstractconfig

import java.lang.{Exception, RuntimeException, Throwable}
import java.nio.file.{FileSystem, Files, Paths}
import java.util.concurrent.locks.{ReadWriteLock, ReentrantReadWriteLock}
import java.util.concurrent.{Callable, Executors, ScheduledExecutorService, ScheduledFuture, TimeUnit}

import com.google.gson.{Gson, JsonObject}
import com.xyniac.environment.Environment
import org.apache.commons.io.IOUtils
import org.json4s._
import org.json4s.native.JsonMethods._
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source
import scala.reflect.runtime.currentMirror
import scala.util.{Failure, Success, Try}

object AbstractConfig {
  val confDirName: String = "conf"

  private val registry = new java.util.concurrent.ConcurrentHashMap[String, AbstractConfig]

  private val logger = LoggerFactory.getLogger(this.getClass)
  private implicit val formats: DefaultFormats.type = DefaultFormats
  private val lock: ReadWriteLock = new ReentrantReadWriteLock
  private val gson: Gson = new Gson()
  private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor
  private [abstractconfig] lazy val handler: ScheduledFuture[String] = scheduler.schedule(task, RemoteConfig.getInitialDelay(), TimeUnit.MILLISECONDS)

  private val task: Callable[String] = () => {
    val fileSystem = Future(RemoteConfig.getFileSystem())
    logger.info("loading the config file system")

    fileSystem.onComplete {
      case Success(fs) => {
        logger.info(s"loaded the config file system $fs")
        logger.info(fileSystem.toString)
        registry.entrySet.parallelStream().forEach(entry => {
          val jsonFileName = entry.getKey

          val hotDeployedDefaultConfigFuture: Future[String] =
            Future(new String(IOUtils.toByteArray(Files.newInputStream(fs.getPath(AbstractConfig.confDirName, jsonFileName)))))
              .recover { case e: Exception => "{}" }

          val hotDeployedEnvConfigFuture: Future[String] =
            Future(new String(IOUtils.toByteArray(Files.newInputStream(fs.getPath(AbstractConfig.confDirName, Environment.env, jsonFileName)))))
              .recover { case e: Exception => "{}" }

          val hotDeployedIaasConfigFuture: Future[String] =
            Future(new String(IOUtils.toByteArray(Files.newInputStream(fs.getPath(AbstractConfig.confDirName, Environment.env, Environment.iaas, jsonFileName)))))
              .recover { case e: Exception => "{}" }

          val hotDeployedRegionConfigFuture: Future[String] =
            Future(new String(IOUtils.toByteArray(Files.newInputStream(fs.getPath(AbstractConfig.confDirName, Environment.env, Environment.iaas, Environment.region, jsonFileName)))))
              .recover { case e: Exception => "{}" }

          val hotDeployedConfigCombined = for {
            hotDeployedDefaultConfig <- hotDeployedDefaultConfigFuture
            hotDeployedEnvConfig <- hotDeployedEnvConfigFuture
            hotDeployedIaasConfig <- hotDeployedIaasConfigFuture
            hotDeployedRegionConfig <- hotDeployedRegionConfigFuture
          } yield pretty(render(parse(hotDeployedDefaultConfig) merge parse(hotDeployedEnvConfig) merge parse(hotDeployedIaasConfig) merge parse(hotDeployedRegionConfig)))

          hotDeployedConfigCombined.onComplete {
            case Success(s) => {
              val latestConfig = gson.fromJson(s, classOf[JsonObject])
              logger.info(s"latestConfig for $jsonFileName:" + latestConfig)
              val currConfig = entry.getValue.getConfigJson()
              if (latestConfig == currConfig) {
                logger.info(s"config remains the same for $jsonFileName")
              } else {
                lock.writeLock().lock()
                try {
                  logger.info("replacing the config to a newer version")
                  entry.getValue.hotDeployedConfig = latestConfig
                } finally {
                  lock.writeLock.unlock()
                }
              }
              scheduler.schedule(task, RemoteConfig.getDelay(), TimeUnit.MILLISECONDS)
            }
            case Failure(e) => {
              logger.error("error parsing the remote config json files", e)
              scheduler.schedule(task, RemoteConfig.getInitialDelay(), TimeUnit.MILLISECONDS)
            }
          }
        })
      }
      case Failure(e) => {
        logger.error("error initiating the file system instance, reschedule remote config reloading task", e)
        scheduler.schedule(task, RemoteConfig.getInitialDelay(), TimeUnit.MILLISECONDS)
      }
    }
    "triggered the task handler"
  }

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

  val jsonFileName: String = this.getClass.getName

  val coldDeployedDefaultConfig: String = Try(Source.fromResource(Paths.get(AbstractConfig.confDirName, jsonFileName).toString).mkString) match {
    case Success(s) => s
    case Failure(e) => "{}"
  }
  val coldDeployedEnvConfig: String = Try(Source.fromResource(Paths.get(AbstractConfig.confDirName, Environment.env, jsonFileName).toString).mkString) match {
    case Success(s) => s
    case Failure(e) => "{}"
  }

  val coldDeployedIaasConfig: String = Try(Source.fromResource(Paths.get(AbstractConfig.confDirName, Environment.env, Environment.iaas, jsonFileName).toString).mkString) match {
    case Success(s) => s
    case Failure(e) => "{}"
  }

  val coldDeployedRegionConfig: String = Try(Source.fromResource(Paths.get(AbstractConfig.confDirName, Environment.env, Environment.iaas, Environment.region, jsonFileName).toString).mkString) match {
    case Success(s) => s
    case Failure(e) => "{}"
  }


  private val coldDeployedConfigJson4j = parse(coldDeployedDefaultConfig) merge parse(coldDeployedEnvConfig) merge parse(coldDeployedIaasConfig) merge parse(coldDeployedRegionConfig)
  private val renderedConfigJson = pretty(render(coldDeployedConfigJson4j))
  private val coldDeployedConfig = AbstractConfig.gson.fromJson(renderedConfigJson, classOf[JsonObject])
  private[abstractconfig] var hotDeployedConfig = new JsonObject()


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
      // TODO: think about handling the null attribute
      val jsonNode = if (hotDeployedConfig.has(key)) hotDeployedConfig.get(key) else coldDeployedConfig.get(key)
      AbstractConfig.gson.fromJson(jsonNode, classType)
    } catch {
      case _: Exception => defaultValue match {
        case None => throw new IllegalArgumentException(s"property $key is defined nowhere")
        case Some(t) => t
      }
    } finally {
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



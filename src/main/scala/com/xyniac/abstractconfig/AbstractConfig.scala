package com.xyniac.abstractconfig

import java.lang.{Exception, RuntimeException, Throwable}
import java.nio.file.{FileSystem, Files, Paths}
import java.util.concurrent.locks.{ReadWriteLock, ReentrantReadWriteLock}
import java.util.concurrent.{Callable, Executors, ScheduledExecutorService, ScheduledFuture, TimeUnit}

import scala.concurrent.duration._
import com.google.gson.{Gson, JsonObject}
import com.xyniac.environment.Environment
import org.apache.commons.io.IOUtils
import org.json4s._
import org.json4s.native.JsonMethods._
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.io.Source
import scala.reflect.runtime.currentMirror
import scala.util.{Failure, Success, Try}
import scala.collection.concurrent.Map
import scala.collection.parallel
import scala.concurrent.blocking

object AbstractConfig {
  val confDirName: String = "conf"

  private val registry = new scala.collection.parallel.mutable.ParHashMap[String, AbstractConfig]
  private val configNames = new java.util.concurrent.CopyOnWriteArrayList[String]
  private val logger = LoggerFactory.getLogger(this.getClass)
  private implicit val formats: DefaultFormats.type = DefaultFormats
  private val lock: ReadWriteLock = new ReentrantReadWriteLock
  private val gson: Gson = new Gson()
  private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor
  private val coldDeployedInitialDelay = getColdConfig("com.xyniac.abstractconfig.RemoteConfig$").get("initialDelay").getAsLong

  private val task: Callable[String] = () => {
    configNames.removeIf(configName => {
      try{
        val module = currentMirror.staticModule(configName)
        val mirror = currentMirror.reflectModule(module)
        val config = mirror.instance.asInstanceOf[AbstractConfig]
        registry.put(configName, config)
        true
      } catch {
        case err: Error => {
          logger.error(s"SEVERE ERROR OCCURRED! will retry loading the config $configName", err)
          false
        }
        case e: Exception=>{
          logger.error(s"exception raised when loading the config $configName", e)
          false
        }
      }
    })

    val fileSystem = Future(RemoteConfig.getFileSystem())
    logger.info("loading the config file system")

    fileSystem.onComplete {
      case Success(fs) => if (fs.isOpen) {
        Try {
          logger.info(s"loaded the config file system $fs")
          logger.info(fileSystem.toString)

          val parallelFuturesKeyConfigString = registry.keys.par.map(key => {
            logger.info(s"scanning the updated config for $key")
            val jsonFileName = key
            val hotDeployedDefaultConfigFuture =
              Future(parse(new String(IOUtils.toByteArray(Files.newInputStream(fs.getPath(AbstractConfig.confDirName, jsonFileName))))))
                .recover { case e: Exception => parse("{}") }

            val hotDeployedEnvConfigFuture =
              Future(parse(new String(IOUtils.toByteArray(Files.newInputStream(fs.getPath(AbstractConfig.confDirName, Environment.env, jsonFileName))))))
                .recover { case e: Exception => parse("{}") }

            val hotDeployedIaasConfigFuture =
              Future(parse(new String(IOUtils.toByteArray(Files.newInputStream(fs.getPath(AbstractConfig.confDirName, Environment.env, Environment.iaas, jsonFileName))))))
                .recover { case e: Exception => parse("{}") }

            val hotDeployedRegionConfigFuture =
              Future(parse(new String(IOUtils.toByteArray(Files.newInputStream(fs.getPath(AbstractConfig.confDirName, Environment.env, Environment.iaas, Environment.region, jsonFileName))))))
                .recover { case e: Exception => parse("{}") }

            val hotDeployedConfigCombinedFuture: Future[String] = for {
              hotDeployedDefaultConfig <- hotDeployedDefaultConfigFuture
              hotDeployedEnvConfig <- hotDeployedEnvConfigFuture
              hotDeployedIaasConfig <- hotDeployedIaasConfigFuture
              hotDeployedRegionConfig <- hotDeployedRegionConfigFuture
            } yield pretty(render(hotDeployedDefaultConfig merge hotDeployedEnvConfig merge hotDeployedIaasConfig merge hotDeployedRegionConfig))
            (key, hotDeployedConfigCombinedFuture)
          })

          val configToReplace = parallelFuturesKeyConfigString.map(futureKeyConfig => {
            val value = Await.result(futureKeyConfig._2, RemoteConfig.getDelay().milliseconds)
            (futureKeyConfig._1, value)
          }).map(keyConfigString => (keyConfigString._1, gson.fromJson(keyConfigString._2, classOf[JsonObject])))
            .filterNot(keyConfigObject => registry(keyConfigObject._1).getConfigJson().equals(keyConfigObject._2))


          if (configToReplace.nonEmpty) {

            logger.info("start updating the hotDeployedConfig pointer")
            try {
              lock.writeLock().lock()
              configToReplace.par.foreach(nc => {
                val obsoletedConfig = registry.get(nc._1)
                obsoletedConfig.get.hotDeployedConfig = nc._2
              })
            } catch {
              case e: Exception => {
                logger.error("error on replacing the config due to: ", e)
              }
              case error: Error => {
                logger.error("SEVERE ERROR OCCURRED! ", error)
              }
            } finally {
              lock.writeLock().unlock()
              logger.info("hotDeployedConfig pointer updating finished")
            }

          }
        } match {
          case Success(s) => {
            logger.info("refreshing task successful")
            scheduler.schedule(task, RemoteConfig.getDelay(), TimeUnit.MILLISECONDS)
          }
          case Failure(e) => {
            logger.error("error loading the config from file system", e)
            scheduler.schedule(task, coldDeployedInitialDelay, TimeUnit.MILLISECONDS)
          }
        }
      } else {
        logger.info("remote file system is closed, retrying")
        scheduler.schedule(task, RemoteConfig.getDelay(), TimeUnit.MILLISECONDS)
      }
      case Failure(e) => {
        logger.error("error initiating the file system instance, reschedule remote config reloading task", e)
        scheduler.schedule(task, coldDeployedInitialDelay, TimeUnit.MILLISECONDS)
      }
    }
    "triggered the task handler"
  }

  private val handler: ScheduledFuture[String] = scheduler.schedule(task, coldDeployedInitialDelay, TimeUnit.MILLISECONDS)

  def checkAllConfig(): JsonObject = {
    val result = new JsonObject
    registry.keys.par.foreach(key => result.add(key, registry(key).getConfigJson()))
    result
  }


  def getColdConfig(jsonFileName: String): JsonObject = {
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


    val coldDeployedConfigJson4j = parse(coldDeployedDefaultConfig) merge parse(coldDeployedEnvConfig) merge parse(coldDeployedIaasConfig) merge parse(coldDeployedRegionConfig)
    val renderedConfigJson = pretty(render(coldDeployedConfigJson4j))
    val coldDeployedConfig = AbstractConfig.gson.fromJson(renderedConfigJson, classOf[JsonObject])
    coldDeployedConfig
  }
  logger.info("Abstract Config Singleton Initializing")
}

abstract class AbstractConfig {

  if (currentMirror.reflect(this).symbol.isModuleClass) {
    AbstractConfig.configNames.add(this.getClass.getCanonicalName)
  } else {
    throw new IllegalStateException("AbstractConfig must be scala singleton")
  }

  val jsonFileName: String = this.getClass.getName
  private val coldDeployedConfig = AbstractConfig.getColdConfig(jsonFileName)
  private[abstractconfig] var hotDeployedConfig:JsonObject = null
  private val remoteConfig = RemoteConfig

  def setProperty[T](key: String, value: T): Unit = {
    AbstractConfig.lock.writeLock().lock()
    try {
      if (hotDeployedConfig==null) {
        hotDeployedConfig = new JsonObject()
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
      val jsonNode = if (hotDeployedConfig!=null) hotDeployedConfig.get(key) else coldDeployedConfig.get(key)
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
    if (hotDeployedConfig==null) {
      coldDeployedConfig
    } else {
      AbstractConfig.lock.readLock.lock()
      val res = hotDeployedConfig.deepCopy()
      AbstractConfig.lock.readLock.unlock()
      res
    }
  }
}



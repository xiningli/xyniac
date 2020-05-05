package com.xyniac.abstractconfig

import java.lang.{Exception, RuntimeException, Throwable}
import java.nio.file.{FileSystem, Files, Paths}
import java.util.concurrent.locks.{ReadWriteLock, ReentrantReadWriteLock}
import java.util.concurrent.{Callable, Executors, ScheduledExecutorService, ScheduledFuture, TimeUnit}
import scala.concurrent.duration._

import scala.jdk.CollectionConverters._
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

object AbstractConfig {
  val confDirName: String = "conf"

  private val registry = new scala.collection.parallel.mutable.ParHashMap[String, AbstractConfig]

  private val logger = LoggerFactory.getLogger(this.getClass)
  private implicit val formats: DefaultFormats.type = DefaultFormats
  private val lock: ReadWriteLock = new ReentrantReadWriteLock
  private val gson: Gson = new Gson()
  private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor
  private[abstractconfig] lazy val handler: ScheduledFuture[String] = {
    logger.info("initial task handler triggered")
    scheduler.schedule(task, RemoteConfig.getInitialDelay(), TimeUnit.MILLISECONDS)
  }

  private val task: Callable[String] = () => {
    val fileSystem = Future(RemoteConfig.getFileSystem())
    logger.info("loading the config file system")

    fileSystem.onComplete {
      case Success(fs) => Try{
        logger.info(s"loaded the config file system $fs")
        logger.info(fileSystem.toString)

//        val configToReplace = new java.util.concurrent.ConcurrentHashMap[String, JsonObject]
        val parallelFuturesKeyConfigString = registry.keys.map(key => {
          logger.info(s"scanning the updated config for $key")
          val jsonFileName = key
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

          val hotDeployedConfigCombinedFuture: Future[String] = for {
            hotDeployedDefaultConfig <- hotDeployedDefaultConfigFuture
            hotDeployedEnvConfig <- hotDeployedEnvConfigFuture
            hotDeployedIaasConfig <- hotDeployedIaasConfigFuture
            hotDeployedRegionConfig <- hotDeployedRegionConfigFuture
          } yield pretty(render(parse(hotDeployedDefaultConfig) merge parse(hotDeployedEnvConfig) merge parse(hotDeployedIaasConfig) merge parse(hotDeployedRegionConfig)))
          (key, hotDeployedConfigCombinedFuture)
        })

        val configToReplace = parallelFuturesKeyConfigString.map(futureKeyConfig => {
          logger.warn("printing the futureKeyConfig" + futureKeyConfig.toString)
          val value = Await.result(futureKeyConfig._2, RemoteConfig.getDelay().milliseconds)
          (futureKeyConfig._1, value)
        }).map(keyConfigString => (keyConfigString._1, gson.fromJson(keyConfigString._2, classOf[JsonObject])))
            .filterNot(keyConfigObject => {
              val flag = registry(keyConfigObject._1).getConfigJson().equals(keyConfigObject._2)
              println(keyConfigObject._1,flag)
              flag
            })



        if (configToReplace.size>0) {
          logger.warn("configToReplace")
          configToReplace.foreach(println(_))

          lock.writeLock().lock()
          try {
            configToReplace.par.foreach(nc => {
              val obsoletedConfig = registry.get(nc._1)
              logger.warn("replacing config of " + nc._1)
              logger.warn(obsoletedConfig.get.hotDeployedConfig + " becomes " + nc._2)
              obsoletedConfig.get.hotDeployedConfig = nc._2
            })
          } catch {
            case e: Exception => {
              e.printStackTrace()
              logger.error("error on replacing the config due to: ", e)
            }
          } finally {
            lock.writeLock().unlock()
          }

        }



      } match {
        case Success(s) => {
          logger.info("refreshing task successful")
          scheduler.schedule(task, RemoteConfig.getDelay(), TimeUnit.MILLISECONDS)
        }
        case Failure(e) => {
          logger.error("error here xxxx", e)
          scheduler.schedule(task, RemoteConfig.getInitialDelay(), TimeUnit.MILLISECONDS)
        }
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
    registry.keys.par.foreach(key => result.add(key, registry(key).getConfigJson()))
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



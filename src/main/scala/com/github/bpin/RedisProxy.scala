package com.github.bpin

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.twitter.finagle.redis.Client
import com.twitter.finagle.http.{Method, Request, Response}
import com.twitter.finagle.redis.util.BufToString
import com.twitter.finagle.{Redis, Service, http}
import com.twitter.util.{Duration, Future}
import com.twitter.io.Buf
import com.twitter.storehaus.cache.Cache
import io.netty.handler.codec.http.HttpResponseStatus


object RedisProxy {
  val logger = LoggerFactory.getLogger(RedisProxy.getClass);
  val redisClient: Client = Redis.newRichClient(Configuration.get_property("redis_server_with_port"))
  var cache: Cache[String, String] = TTLLRUCache(Configuration.get_property("lru_cache_capacity").toLong,
    Duration.fromSeconds(Configuration.get_property("ttl_cache_duration").toInt))

  def lookInRedis(str: String): Future[Option[Buf]] = {
    redisClient.get(Buf.Utf8(str))
  }

  val proxyService: Service[Request, Response] = (request: Request) => {
    logger.info("Logging request information")
    logger.info(s"Incoming request path ${request.path}")
    logger.info(s"Incoming request header ${request.headerMap}")
    request.method match {
      case Method.Get =>
        val key = request.getParam("key")
        logger.info(s"Got HTTP-GET request, Key ${key}")
        if (cache.contains(key)) {
          val cachedVal = cache.get(key)
          cachedVal match {
            case Some(v) =>
              logger.info(s"Get Value ${v} from LRU-TTL Cache")
              cache = cache.hit(key)
              val response = Response()
              response.content(Buf.Utf8(v))
              Future.value(response)
            case None =>
              logger.error(s"Key ${key} from LRU-TTL Cache does not have value")
              val f = lookInRedis(key)
              f flatMap {
                case Some(v) =>
                  val response = Response()
                  response.content(v)
                  Future.value(response)
                case None =>
                  val (_, cache1) = cache.evict(key)
                  cache = cache1
                  val response = Response()
                  Future.value(response.statusCode(404))
              }
          }
        } else {
          val ret = lookInRedis(key)
          ret flatMap {
            case Some(v) =>
              logger.info(s"Get Value ${BufToString(v)} from Redis")
              val (_, cache1) = cache.put((key, BufToString(v)))
              cache = cache1
              val response = Response()
              response.content(v)
              Future.value(response)
            case None =>
              val response = Response()
              Future.value(response.statusCode(404))
          }
        }
      case Method.Post =>
        val key = request.getParam("key")
        val value = request.getParam("value")

        logger.info(s"Got HTTP-POST request, Key ${key}")
        val kB = Buf.Utf8(key)
        val vB = Buf.Utf8(value)
        val response = Response()
        redisClient.set(kB, vB)
          .map { voided => {
            val (_, cache1) = cache.put((key, value))
            cache = cache1
          }
          }
          .map { c => response.statusCode(200) }
    }
  }
}

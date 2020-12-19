package com.segment.assignment

import com.twitter.finagle.redis.Client
import com.twitter.finagle.http.{Method, Request, Response}
import com.twitter.finagle.redis.util.BufToString
import com.twitter.finagle.{Redis, Service, http}
import com.twitter.util.{Duration, Future}
import com.twitter.io.Buf


object RedisProxy {
  val redisClient: Client = Redis.newRichClient(Configuration.get_property("redis_server_with_port"))
  val cache: TTLLRUCache[String, String] = TTLLRUCache(Configuration.get_property("lru_cache_capacity").toLong,
    Duration.fromSeconds(Configuration.get_property("ttl_cache_duration").toInt))

  def lookInRedis(str: String): Future[Option[Buf]] = {
    redisClient.get(Buf.Utf8(str))
  }

  val proxyService: Service[Request, Response] = new Service[http.Request, http.Response] {
    override def apply(request: Request): Future[Response] = {
      request.method match {
        case Method.Get =>
          val key = request.getParam("key")
          println(key)
          if (cache.contains(key)) {
            val cachedVal = cache.get(key)
            cachedVal match {
              case Some(v) =>
                cache.hit(key)
                val response = Response()
                response.content(Buf.Utf8(v))
                Future.value(response)
              case None =>
                val f = lookInRedis(key)
                f flatMap {
                  case Some(v) =>
                    val response = Response()
                    response.content(v)
                    Future.value(response)
                  case None =>
                    cache.evict(key)
                    val response = Response()
                    Future.value(response.statusCode(404))
                }
            }
          } else {
            val ret = lookInRedis(key)
            ret flatMap {
              case Some(v) =>
                cache.put((key, BufToString(v)))
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
          val kB = Buf.Utf8(key)
          val vB = Buf.Utf8(value)
          val response = Response()
          redisClient.set(kB, vB)
          Future.value(response.statusCode(200))
      }
    }
  }
}

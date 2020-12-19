package com.segment.assignment
import java.net.InetSocketAddress

import com.twitter.finagle.{Http, ListeningServer, Redis, Service, http}
import com.twitter.finagle.http.{Method, Request, Response, Status}
import com.twitter.finagle.redis.util.BufToString
import com.twitter.io.Buf
import com.twitter.util.{Await, Future}
import org.scalatest.funspec.AnyFunSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}


class RedisProxyTest extends AnyFunSpec with BeforeAndAfterAll with MockitoSugar with BeforeAndAfterEach {
  var server: ListeningServer = _
  var client: Service[Request, Response] = _
  val testKeyValues: Map[Int, String] = Map(1->"one", 2->"two", 3->"three", 4->"four")

  /**
   * Note this subroutine makes sure I do not flush all of your redis backend
   */
  override def beforeEach(): Unit = {
    testKeyValues
      .foreach(key => RedisProxy.redisClient.dels(Seq(Buf.Utf8(key._1.toString))))
  }

  override def beforeAll(): Unit = {
    println("Test starts")
    // you need to have backend redis server ready
    val httpPort = Configuration.get_property("server_port").asInstanceOf[String].toInt
    val address = new InetSocketAddress(httpPort)
    val service = RedisProxy.proxyService
    println("start http service")
    server = Http.server.serve(address, service)
    try {

      //Await.ready(server)
    } catch {
      case e: InterruptedException => println("Server stopped execution")
      case e: Exception => println("Exception...")
    }
    client = Http.newService(s"localhost:$httpPort")
  }

  override def afterAll(): Unit = {
    println("shutdown http service")
    server.close()
    println("Test done")
  }

  describe("Redis Server Alone SetCommand Test") {
    it("A direct use of redis client to redis server") {
      RedisProxy.redisClient.set(Buf.Utf8("TestKey"), Buf.Utf8("TestValue"))
      assert(RedisProxy.redisClient != null)
      val result = Await.result(RedisProxy.redisClient.get(Buf.Utf8("TestKey"))).get
      assert("TestValue".equals(BufToString(result)))
    }
  }

  describe("Http proxy service key not found test") {
    it("Use HTTP proxy service and redis server") {
      val request = http.Request(Method.Get, "/?key=1")
      val response: Future[http.Response] = client(request)
      val r = Await.result(response)
      assert(r.statusCode == 404)
    }
  }

  describe("Http proxy service write and fetch test") {
    it("Post a key and get it later") {
      val postRequest = http.Request(Method.Post, "/?key=1&value=one")
      val response: Future[http.Response] = client(postRequest)
      val r = Await.result(response)
      assert(r.statusCode == 200)
      val getRequest = http.Request(Method.Get, "/?key=1")
      val getResponse: Future[http.Response] = client(getRequest)
      val result = Await.result(getResponse)
      assert(result.statusCode == 200)
      assert(BufToString(result.content).equals("one"))
    }
    it("Put all and retrieve all") {
      val responses = testKeyValues.flatMap {
        kv => {
          val postReq = http.Request(Method.Post, s"/?key=${kv._1}&value=${kv._2}")
          Seq(client(postReq))
        }
      }
      responses.foreach {
        res => res.map {
          r => assert(r.statusCode == 200)
        }
      }
      val getResponses = testKeyValues.flatMap {
        kv => {
          val getReq = http.Request(Method.Get, s"/?key=${kv._1}")
          Seq(client(getReq))
        }
      }
      getResponses.foreach {
        res => res.map {
          r => {
            assert(r.statusCode == 200)
          }
        }
      }
    }
  }
}

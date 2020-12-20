package com.github.bpin

import java.util.Properties

import com.twitter.finagle.{Http, Service}
import com.twitter.util.{Await, Future}
import java.net.InetSocketAddress
import scala.io.Source

object Main {

  def main(args: Array[String]): Unit = {
    // properties.list(System.out)
    val address = new InetSocketAddress(Configuration.get_property("server_port").asInstanceOf[String].toInt)
    //val service = new ServiceProxy()
    val service = RedisProxy.proxyService
    val server = Http.server.serve(address, service)

    try {
      Await.ready(server)
    } catch {
      case e: InterruptedException => println("Server stopped execution")
      case e: Exception => println("Exception...")
    }
  }
}

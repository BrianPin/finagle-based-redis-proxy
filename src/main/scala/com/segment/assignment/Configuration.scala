package com.segment.assignment

import java.util.Properties

import scala.io.Source

object Configuration {
  val properties: Properties = {
    val prop = new Properties()
    prop.load(Source.fromResource("config.properties").bufferedReader())
    prop
  }

  def get_property(key: String): String = {
    properties.getProperty(key)
  }
}

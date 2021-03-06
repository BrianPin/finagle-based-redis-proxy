name := "redis-proxy"

version := "0.1"

scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.5",
  "org.slf4j" % "slf4j-simple" % "1.7.5",
  "com.twitter" %% "util-core" % "20.5.0",
  "com.twitter" %% "finagle-redis" % "20.5.0",
  "com.twitter" %% "finagle-http" % "20.5.0",
  "com.twitter" %% "storehaus-cache" % "0.15.0",
  "org.scalactic" %% "scalactic" % "3.2.2",
  "org.scalatest" %% "scalatest" % "3.2.2" % "test",
  "org.scalatest" %% "scalatest-wordspec" % "3.2.2" % "test",
  "org.scalatestplus" %% "mockito-3-4" % "3.2.2.0" % "test",
  "org.scalamock" %% "scalamock" % "4.4.0" % "test",
  "org.scalacheck" %% "scalacheck" % "1.14.1" % "test",
  "org.apache.logging.log4j" % "log4j-api" % "2.14.0",
  "org.apache.logging.log4j" % "log4j-core" % "2.14.0",
  "org.apache.logging.log4j" % "log4j-iostreams" % "2.14.0",
  "com.github.kstyrc" % "embedded-redis" % "0.6" % Test
)

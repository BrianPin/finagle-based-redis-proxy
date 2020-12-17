name := "redis-proxy"

version := "0.1"

scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  "com.twitter" %% "util-core" % "20.5.0",
  "com.twitter" %% "finagle-redis" % "20.5.0",
  "com.twitter" %% "finagle-http" % "20.5.0",
  "com.twitter" %% "storehaus-cache" % "0.15.0",
  "org.apache.logging.log4j" % "log4j-api" % "2.14.0",
  "org.apache.logging.log4j" % "log4j-core" % "2.14.0",
  "org.apache.logging.log4j" % "log4j-iostreams" % "2.14.0"
)

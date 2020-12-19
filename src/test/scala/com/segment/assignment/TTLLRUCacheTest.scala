package com.segment.assignment
import org.scalatest.wordspec.AnyWordSpec
import com.twitter.util.Duration

class TTLLRUCacheTest extends AnyWordSpec {
  val ttlMSec: Duration = Duration.fromMilliseconds(2000)
  val maxSize = 2
  val cache: TTLLRUCache[String, Int] = TTLLRUCache[String, Int](maxSize, ttlMSec)

  "The cache has proper TTL eviction" in {
    val (keys, abCache) = cache.putClocked("a" -> 1)._2.putClocked("b" -> 2)
    assert(abCache.toMap == Map("a" -> 1, "b" -> 2))
    Thread.sleep(ttlMSec.inMilliseconds)
    assert(abCache.putClocked("c" -> 3)._2.toMap == Map("c" -> 3))
  }

  "The case has proper LRU eviction" in {
    val (keys, abCache) = cache.putClocked("a" -> 1)._2.putClocked("b" -> 2)
    assert(abCache.toMap == Map("a" -> 1, "b" -> 2))
    assert(abCache.putClocked("c" -> 3)._2.toMap == Map("c" -> 3, "b" -> 2)) // "a" -> 1 was evicted, cachesize = 2
  }
}

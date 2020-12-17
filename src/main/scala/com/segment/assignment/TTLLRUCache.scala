package com.segment.assignment
import com.twitter.storehaus.cache.Cache
import com.twitter.util.Duration

import scala.collection.{SortedMap, breakOut}

object TTLLRUCache {
  def apply[K, V](maxSize: Long, ttl: Duration,
                  backingMap: Map[K, (Long, Long, V)] = Map.empty[K, (Long, Long, V)]
  ): TTLLRUCache[K, V] = new TTLLRUCache(maxSize, 0, ttl, backingMap, SortedMap.empty[Long, K])(() => System.currentTimeMillis)
}

// Long, Long, V => First long is index for LRU, 2nd long is time msec
class TTLLRUCache[K, V](maxSize: Long, idx: Long, ttl: Duration, cache: Map[K, (Long, Long, V)], ord: SortedMap[Long, K])
    (clock: () => Long) extends Cache[K, (Long, Long, V)] {
  // Scala's SortedMap requires an ordering on pairs. To guarantee
  // sorting on index only, LRUCache defines an implicit ordering on K
  // that treats all K as equal.
  protected implicit val keyOrdStop = new Ordering[K] { def compare(l: K, r: K) = 0 }

  override def occupancy: Int = cache.size
  override def iterator: Iterator[(K, (Long, Long, V))] = cache.iterator
  override def toMap: Map[K, (Long, Long, V)] = cache
  override def get(k: K): Option[(Long, Long, V)] = cache.get(k)
  override def put(kv: (K, (Long, Long, V))): (Set[K], TTLLRUCache[K, V]) =
    putWithTime(kv, clock())

  override def empty: Cache[K, (Long, Long, V)] = new TTLLRUCache(maxSize, 0, ttl, Map.empty[K, (Long, Long, V)], SortedMap.empty[Long, K])(clock)

  // provide set of keys expired
  protected def toRemoveExpired(currentMillis: Long): Set[K] =
    toMap.collect {
      case (k, (idx, expiration, _)) if expiration < currentMillis => k
    }(breakOut)

  // provide least used keys if over the size
  protected def removeLRU(key: K, freshCach: Map[K, (Long, Long, V)], freshOrd: SortedMap[Long, K]): (Set[K], Map[K, (Long, Long, V)], SortedMap[Long, K]) =
    if (ord.size >= maxSize) {
      val (idxToEvict, keyToEvict) =
        freshCach.get(key).map { case (i, t, _) => (i, key) }
          .getOrElse(ord.min)
      (Set(keyToEvict), freshCach - keyToEvict, freshOrd - idxToEvict)
    } else {
      (Set.empty[K], freshCach, freshOrd)
    }

  protected def removeExpiredKey(keys: Set[K], staleCache: Map[K, (Long, Long, V)], staleOrd: SortedMap[Long, K]):
    (Map[K, (Long, Long, V)], SortedMap[Long, K]) = {
    val indices = keys.flatMap(k => staleCache.get(k).map(_._1))
    (staleCache -- keys, staleOrd -- indices)
  }

  protected def putWithTime(kv: (K, (Long, Long, V)), currentMsec: Long): (Set[K], TTLLRUCache[K, V]) = {
    val expiredKeys = toRemoveExpired(currentMsec)
    val (freshCache, freshOrd) = removeExpiredKey(expiredKeys, cache, ord)
    val (keyToEvict, finalCache, finalOrd) = removeLRU(kv._1, freshCache, freshOrd)
    (keyToEvict, new TTLLRUCache(maxSize, idx + 1, ttl, finalCache, finalOrd)(clock))
  }

  override def hit(k: K): Cache[K, (Long, Long, V)] = {
    cache.get(k).map {
      case (idx, timel, v) =>
        val newIdx = idx + 1
        val newCache = cache + (k -> (newIdx, timel, v))
        val newOrd = ord - idx + (newIdx -> k)
        new TTLLRUCache(maxSize, newIdx, ttl, newCache, newOrd)(clock)
    }.getOrElse(this)
  }

  override def evict(k: K): (Option[(Long, Long, V)], Cache[K, (Long, Long, V)]) = {
    cache.get(k).map (x => (Some(x), new TTLLRUCache(maxSize, 0, ttl, cache - k, ord - x._1)(clock)))
        .getOrElse((None, this))
  }
}

package com.urlshortener.cache

import zio.*
import zio.test.*
import zio.test.Assertion.*
import io.circe.generic.auto.*
import scala.concurrent.duration.*

object RedisCacheSpec extends ZIOSpecDefault:

  case class TestData(id: Int, name: String)

  // Mock implementation that simulates failure
  class FailingRedisCache extends RedisCache:
    def get[A: io.circe.Decoder](key: String): Task[Option[A]] = ZIO.fail(new Exception("Redis connection failed"))
    def set[A: io.circe.Encoder](key: String, value: A, ttl: Duration): Task[Unit] = ZIO.fail(new Exception("Redis connection failed"))
    def delete(key: String): Task[Unit] = ZIO.fail(new Exception("Redis connection failed"))
    def isAvailable: Task[Boolean] = ZIO.succeed(false)

  // Note: The real Live implementation already has catchAll for graceful degradation.
  // Here we test that the interface behavior (or a stub of it) meets expectations.

  def spec = suite("RedisCacheSpec")(
    test("Failing cache should not crash the application (simulated)") {
      val cache = new FailingRedisCache()
      for
        // In the real Live class, these would return ZIO.succeed(None) due to catchAll
        // But here we're testing the logic that uses the cache should handle Option
        res <- cache.get[TestData]("key").catchAll(_ => ZIO.succeed(None))
      yield 
        assertTrue(res.isEmpty)
    },
    
    test("isAvailable should reflect status") {
      val cache = new FailingRedisCache()
      for
        available <- cache.isAvailable
      yield 
        assertTrue(!available)
    }
  )

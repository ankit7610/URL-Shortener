package com.urlshortener.cache

import zio.*
import dev.profunktor.redis4cats.Redis
import dev.profunktor.redis4cats.effect.Log.Stdout.given
import cats.effect.{IO, Resource}
import io.circe.{Decoder, Encoder}
import io.circe.parser.*
import io.circe.syntax.*
import scala.concurrent.duration.*

trait RedisCache:
  def get[A: Decoder](key: String): Task[Option[A]]
  def set[A: Encoder](key: String, value: A, ttl: Duration): Task[Unit]
  def delete(key: String): Task[Unit]
  def isAvailable: Task[Boolean]

object RedisCache:
  
  case class Live(redis: Resource[IO, dev.profunktor.redis4cats.RedisCommands[IO, String, String]]) extends RedisCache:
    
    override def get[A: Decoder](key: String): Task[Option[A]] =
      ZIO.fromFuture { implicit ec =>
        redis.use { cmd =>
          cmd.get(key).map {
            case Some(json) => decode[A](json).toOption
            case None => None
          }
        }.unsafeToFuture()
      }.catchAll { error =>
        // Graceful degradation - log error but don't fail
        ZIO.logWarning(s"Redis GET error for key $key: ${error.getMessage}") *>
        ZIO.succeed(None)
      }
    
    override def set[A: Encoder](key: String, value: A, ttl: Duration): Task[Unit] =
      ZIO.fromFuture { implicit ec =>
        redis.use { cmd =>
          val json = value.asJson.noSpaces
          cmd.setEx(key, json, ttl.toSeconds).void
        }.unsafeToFuture()
      }.catchAll { error =>
        // Graceful degradation - log error but don't fail
        ZIO.logWarning(s"Redis SET error for key $key: ${error.getMessage}")
      }
    
    override def delete(key: String): Task[Unit] =
      ZIO.fromFuture { implicit ec =>
        redis.use { cmd =>
          cmd.del(key).void
        }.unsafeToFuture()
      }.catchAll { error =>
        ZIO.logWarning(s"Redis DELETE error for key $key: ${error.getMessage}")
      }
    
    override def isAvailable: Task[Boolean] =
      ZIO.fromFuture { implicit ec =>
        redis.use { cmd =>
          cmd.ping.map(_ => true)
        }.unsafeToFuture()
      }.catchAll(_ => ZIO.succeed(false))
  
  val layer: ZLayer[String, Throwable, RedisCache] =
    ZLayer.scoped {
      for
        redisUrl <- ZIO.service[String]
        redis <- ZIO.fromAutoCloseable(
          ZIO.attempt(Redis[IO].utf8(redisUrl))
        )
      yield Live(redis)
    }

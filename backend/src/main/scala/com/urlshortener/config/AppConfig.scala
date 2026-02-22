package com.urlshortener.config

import zio.*
import zio.config.*
import zio.config.typesafe.*
import zio.config.magnolia.*

case class DatabaseConfig(
  url: String,
  poolSize: Int,
  maxOverflow: Int
)

case class RedisConfig(
  url: String,
  maxConnections: Int
)

case class JwtConfig(
  secretKey: String,
  algorithm: String,
  accessTokenExpireMinutes: Int,
  refreshTokenExpireDays: Int
)

case class SecurityConfig(
  rateLimitPerMinute: Int,
  rateLimitPerHour: Int,
  passwordMinLength: Int
)

case class UrlConfig(
  shortUrlLength: Int,
  customAliasMinLength: Int,
  customAliasMaxLength: Int
)

case class CacheConfig(
  ttlHotUrl: Int,
  ttlAnalytics: Int
)

case class AppConfig(
  appName: String,
  appVersion: String,
  environment: String,
  debug: Boolean,
  baseUrl: String,
  allowedOrigins: List[String],
  database: DatabaseConfig,
  redis: RedisConfig,
  jwt: JwtConfig,
  security: SecurityConfig,
  url: UrlConfig,
  cache: CacheConfig,
  sentryDsn: Option[String],
  geoipDatabasePath: Option[String]
):
  def isProduction: Boolean = environment.toLowerCase == "production"
  def isDevelopment: Boolean = environment.toLowerCase == "development"

object AppConfig:
  
  given Config[DatabaseConfig] = deriveConfig[DatabaseConfig]
  given Config[RedisConfig] = deriveConfig[RedisConfig]
  given Config[JwtConfig] = deriveConfig[JwtConfig]
  given Config[SecurityConfig] = deriveConfig[SecurityConfig]
  given Config[UrlConfig] = deriveConfig[UrlConfig]
  given Config[CacheConfig] = deriveConfig[CacheConfig]
  given Config[AppConfig] = deriveConfig[AppConfig]
  
  val layer: ZLayer[Any, Config.Error, AppConfig] =
    ZLayer {
      for
        config <- read(
          deriveConfig[AppConfig].from(
            ConfigSource.fromSystemEnv
              .orElse(ConfigSource.fromSystemProperties)
              .orElse(ConfigSource.fromResourcePath)
          )
        )
      yield config
    }
  
  // Default configuration for development
  val default: AppConfig = AppConfig(
    appName = "URL Shortener",
    appVersion = "1.0.0",
    environment = "development",
    debug = true,
    baseUrl = "http://localhost:8000",
    allowedOrigins = List("http://localhost:3000"),
    database = DatabaseConfig(
      url = "jdbc:postgresql://localhost:5432/urlshortener?user=urlshortener&password=urlshortener",
      poolSize = 20,
      maxOverflow = 10
    ),
    redis = RedisConfig(
      url = "redis://localhost:6379",
      maxConnections = 10
    ),
    jwt = JwtConfig(
      secretKey = "dev-secret-key-change-in-production",
      algorithm = "HS256",
      accessTokenExpireMinutes = 30,
      refreshTokenExpireDays = 7
    ),
    security = SecurityConfig(
      rateLimitPerMinute = 60,
      rateLimitPerHour = 1000,
      passwordMinLength = 8
    ),
    url = UrlConfig(
      shortUrlLength = 7,
      customAliasMinLength = 3,
      customAliasMaxLength = 50
    ),
    cache = CacheConfig(
      ttlHotUrl = 3600,
      ttlAnalytics = 300
    ),
    sentryDsn = None,
    geoipDatabasePath = None
  )

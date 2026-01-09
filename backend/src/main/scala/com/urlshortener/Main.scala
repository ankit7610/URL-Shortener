package com.urlshortener

import zio.*
import zio.interop.catz.*
import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import org.http4s.server.middleware.{CORS, Logger as HttpLogger}
import com.comcast.ip4s.*
import doobie.*
import doobie.hikari.HikariTransactor
import com.urlshortener.config.AppConfig
import com.urlshortener.repository.{UserRepository, URLRepository, AnalyticsRepository}
import com.urlshortener.service.{URLShortenerService, SecurityService, AnalyticsService, QRCodeService}
import com.urlshortener.cache.RedisCache
import com.urlshortener.api.{URLRoutes, AuthRoutes, HealthRoutes}

object Main extends ZIOAppDefault:
  
  // Database transactor layer
  val transactorLayer: ZLayer[AppConfig, Throwable, Transactor[Task]] =
    ZLayer.scoped {
      for
        config <- ZIO.service[AppConfig]
        xa <- ZIO.fromAutoCloseable(
          ZIO.attempt {
            HikariTransactor.newHikariTransactor[Task](
              driverClassName = "org.postgresql.Driver",
              url = config.database.url,
              user = "",
              pass = "",
              connectEC = scala.concurrent.ExecutionContext.global
            ).allocated.unsafeRunSync()._1
          }
        )
      yield xa
    }
  
  // Redis URL layer
  val redisUrlLayer: ZLayer[AppConfig, Nothing, String] =
    ZLayer.fromZIO(ZIO.serviceWith[AppConfig](_.redis.url))
  
  // Complete application layer
  val appLayer: ZLayer[Any, Throwable, AppConfig & Transactor[Task] & RedisCache & 
    UserRepository & URLRepository & AnalyticsRepository &
    URLShortenerService & SecurityService & AnalyticsService & QRCodeService] =
    ZLayer.make[AppConfig & Transactor[Task] & RedisCache & 
      UserRepository & URLRepository & AnalyticsRepository &
      URLShortenerService & SecurityService & AnalyticsService & QRCodeService](
      // Config
      AppConfig.layer.orDie,
      
      // Infrastructure
      transactorLayer,
      redisUrlLayer,
      RedisCache.layer.orDie,
      
      // Repositories
      UserRepository.layer,
      URLRepository.layer,
      AnalyticsRepository.layer,
      
      // Services
      URLShortenerService.layer,
      SecurityService.layer,
      AnalyticsService.layer,
      QRCodeService.layer
    )
  
  // HTTP server
  def server(using runtime: Runtime[Any]): IO[Nothing] =
    val routes = Router(
      "/" -> (URLRoutes.routes[IO] <+> AuthRoutes.routes[IO] <+> HealthRoutes.routes[IO])
    ).orNotFound
    
    // Apply middleware
    val corsRoutes = CORS.policy.withAllowOriginAll(routes)
    val loggedRoutes = HttpLogger.httpRoutes(logHeaders = true, logBody = false)(corsRoutes)
    
    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8000")
      .withHttpApp(loggedRoutes)
      .build
      .useForever
      .as(ExitCode.Success)
  
  override def run: ZIO[Any, Any, Any] =
    ZIO.scoped {
      for
        _ <- ZIO.logInfo("Starting URL Shortener...")
        
        // Provide all dependencies
        _ <- ZIO.serviceWithZIO[AppConfig] { config =>
          ZIO.logInfo(s"Environment: ${config.environment}") *>
          ZIO.logInfo(s"Version: ${config.appVersion}")
        }
        
        // Start HTTP server
        runtime <- ZIO.runtime[Any]
        _ <- ZIO.fromFuture(_ => server(using runtime).unsafeToFuture())
      yield ()
    }.provide(appLayer)

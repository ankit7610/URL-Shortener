package com.urlshortener.api

import zio.*
import org.http4s.*
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityCodec.*
import com.urlshortener.dto.*
import com.urlshortener.dto.DTOCodecs.given
import com.urlshortener.config.AppConfig
import com.urlshortener.cache.RedisCache
import doobie.Transactor
import cats.effect.IO

object HealthRoutes:
  
  def routes[F[_]: Async](using runtime: Runtime[Any]): HttpRoutes[F] =
    val dsl = Http4sDsl[F]
    import dsl.*
    
    HttpRoutes.of[F] {
      
      // Health check
      case GET -> Root / "health" =>
        val program = for
          config <- ZIO.service[AppConfig]
          cache <- ZIO.service[RedisCache]
          xa <- ZIO.service[Transactor[Task]]
          
          // Check database
          dbStatus <- ZIO.fromFuture { implicit ec =>
            import doobie.implicits.*
            sql"SELECT 1".query[Int].unique.transact(xa).unsafeToFuture()
          }.as("healthy").catchAll(_ => ZIO.succeed("unhealthy"))
          
          // Check cache
          cacheAvailable <- cache.isAvailable
          cacheStatus = if cacheAvailable then "healthy" else "unavailable"
          
          overallStatus = if dbStatus == "healthy" then "healthy" else "degraded"
          
          response = HealthResponse(
            status = overallStatus,
            version = config.appVersion,
            environment = config.environment,
            database = dbStatus,
            cache = cacheStatus
          )
        yield response
        
        Unsafe.unsafe { implicit unsafe =>
          runtime.unsafe.runToFuture(program).flatMap {
            case scala.util.Success(response) => Ok(response)
            case scala.util.Failure(ex) => InternalServerError(ErrorResponse(ex.getMessage))
          }
        }
      
      // Root endpoint
      case GET -> Root =>
        val program = for
          config <- ZIO.service[AppConfig]
          response = Map(
            "name" -> config.appName,
            "version" -> config.appVersion,
            "docs" -> (if config.debug then Some("/docs") else None),
            "health" -> "/health"
          )
        yield response
        
        Unsafe.unsafe { implicit unsafe =>
          runtime.unsafe.runToFuture(program).flatMap {
            case scala.util.Success(response) => Ok(response)
            case scala.util.Failure(ex) => InternalServerError(ErrorResponse(ex.getMessage))
          }
        }
    }

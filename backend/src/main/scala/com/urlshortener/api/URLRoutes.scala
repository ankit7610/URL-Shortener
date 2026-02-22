package com.urlshortener.api

import zio.*
import org.http4s.*
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityCodec.*
import com.urlshortener.dto.*
import com.urlshortener.dto.DTOCodecs.given
import com.urlshortener.domain.{User, URL as DomainURL}
import com.urlshortener.repository.{UserRepository, URLRepository}
import com.urlshortener.service.{URLShortenerService, AnalyticsService, QRCodeService, SecurityService}
import com.urlshortener.cache.RedisCache
import com.urlshortener.config.AppConfig
import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.concurrent.duration.*

object URLRoutes:
  
  def routes[F[_]: Async](using runtime: Runtime[Any]): HttpRoutes[F] =
    val dsl = Http4sDsl[F]
    import dsl.*
    
    HttpRoutes.of[F] {
      
      // Create short URL
      case req @ POST -> Root / "api" / "urls" =>
        val program = for
          createReq <- req.as[URLCreateRequest]
          config <- ZIO.service[AppConfig]
          urlService <- ZIO.service[URLShortenerService]
          urlRepo <- ZIO.service[URLRepository]
          securityService <- ZIO.service[SecurityService]
          
          // Validate URL
          _ <- ZIO.fromEither(urlService.validateUrl(createReq.originalUrl))
            .mapError(msg => new Exception(msg))
          
          // Check custom alias if provided
          _ <- createReq.customAlias match
            case Some(alias) =>
              for
                _ <- ZIO.fromEither(urlService.validateCustomAlias(alias))
                  .mapError(msg => new Exception(msg))
                existing <- urlRepo.findByCustomAlias(alias)
                _ <- ZIO.when(existing.isDefined)(
                  ZIO.fail(new Exception("Custom alias already exists"))
                )
              yield ()
            case None => ZIO.unit
          
          // Calculate expiration
          expiresAt = createReq.expiresInDays.map(days =>
            Instant.now().plus(days.toLong, ChronoUnit.DAYS)
          )
          
          // Hash password if provided
          passwordHash <- createReq.password match
            case Some(pwd) => securityService.hashPassword(pwd).map(Some(_))
            case None => ZIO.succeed(None)
          
          // Create URL with temporary short code
          newUrl = DomainURL.create(
            shortCode = "temp",
            originalUrl = createReq.originalUrl,
            customAlias = createReq.customAlias,
            title = createReq.title,
            description = createReq.description,
            userId = None, // TODO: Get from auth
            passwordHash = passwordHash,
            expiresAt = expiresAt
          )
          
          // Save to get ID
          savedUrl <- urlRepo.create(newUrl)
          
          // Generate short code from ID
          shortCode = urlService.generateShortCode(savedUrl.id)
          
          // Update with actual short code
          finalUrl <- urlRepo.update(savedUrl.copy(shortCode = shortCode))
          
          // Build response
          baseUrl = config.baseUrl
          response = URLResponse(
            id = finalUrl.id,
            shortCode = finalUrl.shortCode,
            originalUrl = finalUrl.originalUrl,
            shortUrl = s"$baseUrl/${finalUrl.customAlias.getOrElse(finalUrl.shortCode)}",
            customAlias = finalUrl.customAlias,
            title = finalUrl.title,
            description = finalUrl.description,
            isPasswordProtected = finalUrl.isPasswordProtected,
            isActive = finalUrl.isActive,
            expiresAt = finalUrl.expiresAt,
            clickCount = finalUrl.clickCount,
            createdAt = finalUrl.createdAt,
            qrCodeUrl = s"$baseUrl/api/urls/${finalUrl.id}/qr"
          )
        yield response
        
        Unsafe.unsafe { implicit unsafe =>
          runtime.unsafe.runToFuture(program).flatMap {
            case scala.util.Success(response) => Created(response)
            case scala.util.Failure(ex) => BadRequest(ErrorResponse(ex.getMessage))
          }
        }
      
      // Redirect short URL
      case GET -> Root / shortCode =>
        val program = for
          urlRepo <- ZIO.service[URLRepository]
          cache <- ZIO.service[RedisCache]
          analyticsService <- ZIO.service[AnalyticsService]
          
          // Try cache first
          cachedUrl <- cache.get[DomainURL](s"url:$shortCode")
          
          // Get from DB if not cached
          url <- cachedUrl match
            case Some(url) => ZIO.succeed(url)
            case None =>
              for
                maybeUrl <- urlRepo.findByShortCode(shortCode)
                  .flatMap {
                    case Some(u) => ZIO.succeed(u)
                    case None => urlRepo.findByCustomAlias(shortCode)
                  }
                url <- ZIO.fromOption(maybeUrl)
                  .orElseFail(new Exception("Short URL not found"))
                
                // Cache for future requests
                _ <- cache.set(s"url:$shortCode", url, 1.hour)
              yield url
          
          // Check if accessible
          _ <- ZIO.when(!url.isAccessible)(
            if url.isExpired then
              ZIO.fail(new Exception("This link has expired"))
            else
              ZIO.fail(new Exception("This link is no longer active"))
          )
          
          // Track analytics (async, don't block)
          _ <- analyticsService.trackClick(
            urlId = url.id,
            ipAddress = None, // TODO: Extract from request
            userAgent = None, // TODO: Extract from request
            referrer = None
          ).forkDaemon
          
        yield url.originalUrl
        
        Unsafe.unsafe { implicit unsafe =>
          runtime.unsafe.runToFuture(program).flatMap {
            case scala.util.Success(originalUrl) =>
              SeeOther(Location(Uri.unsafeFromString(originalUrl)))
            case scala.util.Failure(ex) =>
              NotFound(ErrorResponse(ex.getMessage))
          }
        }
      
      // Get URL analytics
      case GET -> Root / "api" / "urls" / LongVar(urlId) / "analytics" =>
        val program = for
          analyticsService <- ZIO.service[AnalyticsService]
          stats <- analyticsService.getUrlAnalytics(urlId, 30)
          response = AnalyticsSummary(
            totalClicks = stats.totalClicks,
            uniqueVisitors = stats.uniqueVisitors,
            topCountries = stats.topCountries,
            topDevices = stats.topDevices,
            topBrowsers = stats.topBrowsers,
            clicksByDay = stats.clicksByDay
          )
        yield response
        
        Unsafe.unsafe { implicit unsafe =>
          runtime.unsafe.runToFuture(program).flatMap {
            case scala.util.Success(response) => Ok(response)
            case scala.util.Failure(ex) => InternalServerError(ErrorResponse(ex.getMessage))
          }
        }
      
      // Generate QR code
      case GET -> Root / "api" / "urls" / LongVar(urlId) / "qr" =>
        val program = for
          urlRepo <- ZIO.service[URLRepository]
          qrService <- ZIO.service[QRCodeService]
          
          url <- urlRepo.findById(urlId)
            .someOrFail(new Exception("URL not found"))
          
          baseUrl = config.baseUrl
          shortUrl = s"$baseUrl/${url.customAlias.getOrElse(url.shortCode)}"
          
          qrBytes <- qrService.generateQRCode(shortUrl, 300, 2)
        yield qrBytes
        
        Unsafe.unsafe { implicit unsafe =>
          runtime.unsafe.runToFuture(program).flatMap {
            case scala.util.Success(bytes) =>
              Ok(bytes)
                .map(_.withContentType(MediaType.image.png))
            case scala.util.Failure(ex) =>
              InternalServerError(ErrorResponse(ex.getMessage))
          }
        }
    }

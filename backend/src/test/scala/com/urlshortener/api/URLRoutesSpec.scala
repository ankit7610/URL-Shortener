package com.urlshortener.api

import zio.*
import zio.test.*
import zio.test.Assertion.*
import org.http4s.*
import org.http4s.implicits.*
import org.http4s.circe.CirceEntityCodec.*
import com.urlshortener.dto.*
import com.urlshortener.dto.DTOCodecs.given
import com.urlshortener.domain.{URL => DomainURL}
import com.urlshortener.repository.URLRepository
import com.urlshortener.service.{URLShortenerService, AnalyticsService, QRCodeService, SecurityService}
import com.urlshortener.cache.RedisCache
import com.urlshortener.config.AppConfig
import cats.effect.IO
import zio.interop.catz.*
import java.time.Instant

object URLRoutesSpec extends ZIOSpecDefault:

  val mockUrlService = ZLayer.succeed(new URLShortenerService {
    def encode(num: Long): String = num.toString
    def decode(shortCode: String): Option[Long] = shortCode.toLongOption
    def generateShortCode(urlId: Long, retryCount: Int): String = urlId.toString
    def validateCustomAlias(alias: String): Either[String, Unit] = Right(())
    def validateUrl(url: String): Either[String, Unit] = if url.startsWith("http") then Right(()) else Left("Invalid URL")
  })

  val mockAnalyticsService = ZLayer.succeed(new AnalyticsService {
    def trackClick(urlId: Long, ip: Option[String], ua: Option[String], ref: Option[String]): Task[Unit] = ZIO.unit
    def getUrlAnalytics(urlId: Long, days: Int): Task[com.urlshortener.domain.ClickStats] = ZIO.succeed(
      com.urlshortener.domain.ClickStats(10L, 5L, Nil, Nil, Nil, Nil)
    )
  })

  val mockUrlRepo = ZLayer.fromZIO(Ref.make(Map.empty[Long, DomainURL]).map { ref =>
    new URLRepository {
      def findById(id: Long): Task[Option[DomainURL]] = ref.get.map(_.get(id))
      def findByShortCode(shortCode: String): Task[Option[DomainURL]] = ref.get.map(_.values.find(_.shortCode == shortCode))
      def findByCustomAlias(alias: String): Task[Option[DomainURL]] = ref.get.map(_.values.find(_.customAlias.contains(alias)))
      def findByUserId(userId: Long, offset: Int, limit: Int): Task[List[DomainURL]] = ZIO.succeed(Nil)
      def searchByUserId(userId: Long, query: String, offset: Int, limit: Int): Task[List[DomainURL]] = ZIO.succeed(Nil)
      def countByUserId(userId: Long): Task[Long] = ZIO.succeed(0L)
      def create(url: DomainURL): Task[DomainURL] = 
        val saved = url.copy(id = 1L)
        ref.update(_ + (1L -> saved)).as(saved)
      def update(url: DomainURL): Task[DomainURL] = ref.update(_ + (url.id -> url)).as(url)
      def delete(id: Long): Task[Unit] = ref.update(_ - id)
      def incrementClickCount(id: Long): Task[Unit] = ZIO.unit
      def updateLastAccessed(id: Long): Task[Unit] = ZIO.unit
    }
  })

  val mockCache = ZLayer.succeed(new RedisCache {
    def get[A: io.circe.Decoder](key: String): Task[Option[A]] = ZIO.succeed(None)
    def set[A: io.circe.Encoder](key: String, value: A, ttl: scala.concurrent.duration.Duration): Task[Unit] = ZIO.unit
    def delete(key: String): Task[Unit] = ZIO.unit
    def isAvailable: Task[Boolean] = ZIO.succeed(true)
  })

  val mockQrService = ZLayer.succeed(new QRCodeService {
    def generateQRCode(url: String, size: Int, border: Int): Task[Array[Byte]] = ZIO.succeed(Array(1, 2, 3))
  })

  val mockSecurityService = ZLayer.succeed(new SecurityService {
    def hashPassword(password: String): Task[String] = ZIO.succeed(password)
    def verifyPassword(p: String, h: String): Task[Boolean] = ZIO.succeed(true)
    def createAccessToken(u: Long, e: String): Task[String] = ???
    def createRefreshToken(u: Long, e: String): Task[String] = ???
    def decodeToken(t: String): Task[Option[(Long, String)]] = ???
    def validatePasswordStrength(p: String): Either[String, Unit] = Right(())
  })

  def spec = suite("URLRoutesSpec")(
    test("POST /api/urls should create a short URL") {
      for
        runtime <- ZIO.runtime[Any]
        given Runtime[Any] = runtime
        routes = URLRoutes.routes[IO]
        request = Request[IO](Method.POST, uri"/api/urls")
          .withEntity(URLCreateRequest("https://google.com"))
        response <- routes.orNotFound.run(request)
        body <- response.as[URLResponse]
      yield 
        assertTrue(response.status == Status.Created) &&
        assertTrue(body.originalUrl == "https://google.com") &&
        assertTrue(body.shortCode == "1")
    },

    test("GET /{shortCode} should redirect") {
      for
        runtime <- ZIO.runtime[Any]
        given Runtime[Any] = runtime
        routes = URLRoutes.routes[IO]
        urlRepo <- ZIO.service[URLRepository]
        _ <- urlRepo.create(DomainURL.create("test", "https://bing.com"))
        
        request = Request[IO](Method.GET, uri"/test")
        response <- routes.orNotFound.run(request)
      yield 
        assertTrue(response.status == Status.SeeOther) &&
        assertTrue(response.headers.get[org.http4s.headers.Location].get.uri.renderString == "https://bing.com")
    },

    test("GET /api/urls/{id}/analytics should return stats") {
      for
        runtime <- ZIO.runtime[Any]
        given Runtime[Any] = runtime
        routes = URLRoutes.routes[IO]
        request = Request[IO](Method.GET, uri"/api/urls/1/analytics")
        response <- routes.orNotFound.run(request)
        body <- response.as[AnalyticsSummary]
      yield 
        assertTrue(response.status == Status.Ok) &&
        assertTrue(body.totalClicks == 10L)
    }
  ).provide(
    ZLayer.succeed(AppConfig.default),
    mockUrlService,
    mockUrlRepo,
    mockAnalyticsService,
    mockCache,
    mockQrService,
    mockSecurityService
  )

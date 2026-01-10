package com.urlshortener.integration

import zio.*
import zio.test.*
import zio.test.Assertion.*
import org.http4s.*
import org.http4s.implicits.*
import org.http4s.circe.CirceEntityCodec.*
import com.urlshortener.dto.*
import com.urlshortener.dto.DTOCodecs.given
import com.urlshortener.api.{URLRoutes, AuthRoutes}
import com.urlshortener.config.AppConfig
import com.urlshortener.repository.*
import com.urlshortener.service.*
import com.urlshortener.cache.RedisCache
import cats.effect.IO
import zio.interop.catz.*

object URLFlowIntegrationSpec extends ZIOSpecDefault:
  
  // Mock cache for integration tests
  val mockCache = ZLayer.succeed(new RedisCache {
    def get[A: io.circe.Decoder](key: String): Task[Option[A]] = ZIO.succeed(None)
    def set[A: io.circe.Encoder](key: String, value: A, ttl: scala.concurrent.duration.Duration): Task[Unit] = ZIO.unit
    def delete(key: String): Task[Unit] = ZIO.unit
    def isAvailable: Task[Boolean] = ZIO.succeed(true)
  })
  
  def spec = suite("URLFlowIntegrationSpec")(
    test("complete URL creation and redirect flow") {
      for
        runtime <- ZIO.runtime[Any]
        given Runtime[Any] = runtime
        
        urlRoutes = URLRoutes.routes[IO]
        
        // Create a URL
        createRequest = Request[IO](Method.POST, uri"/api/urls")
          .withEntity(URLCreateRequest("https://github.com/scala/scala"))
        
        createResponse <- urlRoutes.orNotFound.run(createRequest)
        urlResponse <- createResponse.as[URLResponse]
        
        // Verify creation
        _ = assertTrue(createResponse.status == Status.Created)
        _ = assertTrue(urlResponse.originalUrl == "https://github.com/scala/scala")
        
        // Test redirect
        redirectRequest = Request[IO](Method.GET, Uri.unsafeFromString(s"/${urlResponse.shortCode}"))
        redirectResponse <- urlRoutes.orNotFound.run(redirectRequest)
      yield 
        assertTrue(redirectResponse.status == Status.SeeOther) &&
        assertTrue(
          redirectResponse.headers.get[org.http4s.headers.Location]
            .map(_.uri.renderString)
            .contains("https://github.com/scala/scala")
        )
    },
    
    test("URL with custom alias should redirect correctly") {
      for
        runtime <- ZIO.runtime[Any]
        given Runtime[Any] = runtime
        
        urlRoutes = URLRoutes.routes[IO]
        
        createRequest = Request[IO](Method.POST, uri"/api/urls")
          .withEntity(URLCreateRequest(
            originalUrl = "https://zio.dev",
            customAlias = Some("my-zio-link")
          ))
        
        createResponse <- urlRoutes.orNotFound.run(createRequest)
        urlResponse <- createResponse.as[URLResponse]
        
        // Redirect using custom alias
        redirectRequest = Request[IO](Method.GET, uri"/my-zio-link")
        redirectResponse <- urlRoutes.orNotFound.run(redirectRequest)
      yield 
        assertTrue(urlResponse.customAlias == Some("my-zio-link")) &&
        assertTrue(redirectResponse.status == Status.SeeOther)
    },
    
    test("invalid URL should be rejected") {
      for
        runtime <- ZIO.runtime[Any]
        given Runtime[Any] = runtime
        
        urlRoutes = URLRoutes.routes[IO]
        
        createRequest = Request[IO](Method.POST, uri"/api/urls")
          .withEntity(URLCreateRequest("javascript:alert('xss')"))
        
        createResponse <- urlRoutes.orNotFound.run(createRequest)
      yield 
        assertTrue(createResponse.status == Status.BadRequest)
    },
    
    test("QR code generation should work") {
      for
        runtime <- ZIO.runtime[Any]
        given Runtime[Any] = runtime
        
        urlRoutes = URLRoutes.routes[IO]
        urlRepo <- ZIO.service[URLRepository]
        
        // Create URL first
        url = com.urlshortener.domain.URL.create("qr1", "https://example.com")
        created <- urlRepo.create(url)
        
        // Get QR code
        qrRequest = Request[IO](Method.GET, Uri.unsafeFromString(s"/api/urls/${created.id}/qr"))
        qrResponse <- urlRoutes.orNotFound.run(qrRequest)
        qrBytes <- qrResponse.as[Array[Byte]]
      yield 
        assertTrue(qrResponse.status == Status.Ok) &&
        assertTrue(qrBytes.nonEmpty)
    }
  ).provide(
    ZLayer.succeed(AppConfig.default),
    DatabaseTestUtils.createTestDatabase,
    URLRepository.layer,
    UserRepository.layer,
    AnalyticsRepository.layer,
    URLShortenerService.layer,
    SecurityService.layer,
    AnalyticsService.layer,
    QRCodeService.layer,
    mockCache
  ) @@ TestAspect.beforeEach(
    ZIO.serviceWithZIO[doobie.Transactor[Task]](DatabaseTestUtils.cleanDatabase)
  ) @@ TestAspect.sequential

package com.urlshortener.api

import zio.*
import zio.test.*
import zio.test.Assertion.*
import org.http4s.*
import org.http4s.implicits.*
import org.http4s.circe.CirceEntityCodec.*
import com.urlshortener.dto.*
import com.urlshortener.dto.DTOCodecs.given
import com.urlshortener.domain.User
import com.urlshortener.repository.UserRepository
import com.urlshortener.service.SecurityService
import com.urlshortener.config.AppConfig
import cats.effect.IO
import zio.interop.catz.*
import java.time.Instant

object AuthRoutesSpec extends ZIOSpecDefault:

  val mockSecurityService = ZLayer.succeed(new SecurityService {
    def hashPassword(password: String): Task[String] = ZIO.succeed("hashed_" + password)
    def verifyPassword(plain: String, hashed: String): Task[Boolean] = ZIO.succeed(hashed == "hashed_" + plain)
    def createAccessToken(userId: Long, email: String): Task[String] = ZIO.succeed("access_token")
    def createRefreshToken(userId: Long, email: String): Task[String] = ZIO.succeed("refresh_token")
    def decodeToken(token: String): Task[Option[(Long, String)]] = ZIO.succeed(Some((1L, "test@example.com")))
    def validatePasswordStrength(password: String): Either[String, Unit] = if password.length >= 8 then Right(()) else Left("Too short")
  })

  val mockUserRepo = ZLayer.fromZIO(Ref.make(Map.empty[String, User]).map { ref =>
    new UserRepository {
      def findById(id: Long): Task[Option[User]] = ZIO.succeed(None)
      def findByEmail(email: String): Task[Option[User]] = ref.get.map(_.get(email))
      def findByApiKey(apiKey: String): Task[Option[User]] = ZIO.succeed(None)
      def create(user: User): Task[User] = 
        val saved = user.copy(id = 1L)
        ref.update(_ + (user.email -> saved)).as(saved)
      def update(user: User): Task[User] = ZIO.succeed(user)
      def delete(id: Long): Task[Unit] = ZIO.unit
      def updateLastLogin(id: Long): Task[Unit] = ZIO.unit
    }
  })

  def spec = suite("AuthRoutesSpec")(
    test("POST /auth/register should create a user and return tokens") {
      for
        runtime <- ZIO.runtime[Any]
        given Runtime[Any] = runtime
        routes = AuthRoutes.routes[IO]
        request = Request[IO](Method.POST, uri"/auth/register")
          .withEntity(RegisterRequest("test@example.com", "StrongPass123", Some("Test User")))
        response <- routes.orNotFound.run(request)
        body <- response.as[TokenResponse]
      yield 
        assertTrue(response.status == Status.Created) &&
        assertTrue(body.accessToken == "access_token") &&
        assertTrue(body.refreshToken == "refresh_token")
    },

    test("POST /auth/login should return tokens for valid credentials") {
      for
        runtime <- ZIO.runtime[Any]
        given Runtime[Any] = runtime
        routes = AuthRoutes.routes[IO]
        userRepo <- ZIO.service[UserRepository]
        _ <- userRepo.create(User(1L, "login@example.com", "hashed_pass123", None, true, false, false, None, Instant.now(), Instant.now(), None))
        
        request = Request[IO](Method.POST, uri"/auth/login")
          .withEntity(LoginRequest("login@example.com", "pass123"))
        response <- routes.orNotFound.run(request)
        body <- response.as[TokenResponse]
      yield 
        assertTrue(response.status == Status.Ok) &&
        assertTrue(body.accessToken == "access_token")
    },

    test("POST /auth/login should fail for wrong credentials") {
      for
        runtime <- ZIO.runtime[Any]
        given Runtime[Any] = runtime
        routes = AuthRoutes.routes[IO]
        request = Request[IO](Method.POST, uri"/auth/login")
          .withEntity(LoginRequest("nonexistent@example.com", "wrong"))
        response <- routes.orNotFound.run(request)
      yield 
        assertTrue(response.status == Status.Unauthorized)
    }
  ).provide(
    ZLayer.succeed(AppConfig.default),
    mockSecurityService,
    mockUserRepo
  )

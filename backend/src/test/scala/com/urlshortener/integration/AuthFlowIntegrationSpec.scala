package com.urlshortener.integration

import zio.*
import zio.test.*
import zio.test.Assertion.*
import org.http4s.*
import org.http4s.implicits.*
import org.http4s.circe.CirceEntityCodec.*
import com.urlshortener.dto.*
import com.urlshortener.dto.DTOCodecs.given
import com.urlshortener.api.AuthRoutes
import com.urlshortener.config.AppConfig
import com.urlshortener.repository.UserRepository
import com.urlshortener.service.SecurityService
import cats.effect.IO
import zio.interop.catz.*

object AuthFlowIntegrationSpec extends ZIOSpecDefault:
  
  def spec = suite("AuthFlowIntegrationSpec")(
    test("complete registration and login flow") {
      for
        runtime <- ZIO.runtime[Any]
        given Runtime[Any] = runtime
        
        authRoutes = AuthRoutes.routes[IO]
        
        // Register
        registerRequest = Request[IO](Method.POST, uri"/auth/register")
          .withEntity(RegisterRequest(
            email = "integration@test.com",
            password = "SecurePass123!",
            fullName = Some("Integration Test")
          ))
        
        registerResponse <- authRoutes.orNotFound.run(registerRequest)
        registerTokens <- registerResponse.as[TokenResponse]
        
        _ = assertTrue(registerResponse.status == Status.Created)
        _ = assertTrue(registerTokens.accessToken.nonEmpty)
        
        // Login with same credentials
        loginRequest = Request[IO](Method.POST, uri"/auth/login")
          .withEntity(LoginRequest(
            email = "integration@test.com",
            password = "SecurePass123!"
          ))
        
        loginResponse <- authRoutes.orNotFound.run(loginRequest)
        loginTokens <- loginResponse.as[TokenResponse]
      yield 
        assertTrue(loginResponse.status == Status.Ok) &&
        assertTrue(loginTokens.accessToken.nonEmpty) &&
        assertTrue(loginTokens.refreshToken.nonEmpty)
    },
    
    test("registration with weak password should fail") {
      for
        runtime <- ZIO.runtime[Any]
        given Runtime[Any] = runtime
        
        authRoutes = AuthRoutes.routes[IO]
        
        registerRequest = Request[IO](Method.POST, uri"/auth/register")
          .withEntity(RegisterRequest(
            email = "weak@test.com",
            password = "weak",
            fullName = None
          ))
        
        registerResponse <- authRoutes.orNotFound.run(registerRequest)
      yield 
        assertTrue(registerResponse.status == Status.BadRequest)
    },
    
    test("duplicate email registration should fail") {
      for
        runtime <- ZIO.runtime[Any]
        given Runtime[Any] = runtime
        
        authRoutes = AuthRoutes.routes[IO]
        
        registerRequest = Request[IO](Method.POST, uri"/auth/register")
          .withEntity(RegisterRequest(
            email = "duplicate@test.com",
            password = "SecurePass123!",
            fullName = None
          ))
        
        // First registration
        firstResponse <- authRoutes.orNotFound.run(registerRequest)
        _ = assertTrue(firstResponse.status == Status.Created)
        
        // Second registration with same email
        secondResponse <- authRoutes.orNotFound.run(registerRequest)
      yield 
        assertTrue(secondResponse.status == Status.BadRequest)
    },
    
    test("login with wrong password should fail") {
      for
        runtime <- ZIO.runtime[Any]
        given Runtime[Any] = runtime
        
        authRoutes = AuthRoutes.routes[IO]
        userRepo <- ZIO.service[UserRepository]
        securityService <- ZIO.service[SecurityService]
        
        // Create user directly
        hashedPassword <- securityService.hashPassword("CorrectPass123!")
        user = com.urlshortener.domain.User.create(
          "wrongpass@test.com",
          hashedPassword,
          None
        )
        _ <- userRepo.create(user)
        
        // Try to login with wrong password
        loginRequest = Request[IO](Method.POST, uri"/auth/login")
          .withEntity(LoginRequest(
            email = "wrongpass@test.com",
            password = "WrongPass123!"
          ))
        
        loginResponse <- authRoutes.orNotFound.run(loginRequest)
      yield 
        assertTrue(loginResponse.status == Status.Unauthorized)
    },
    
    test("login with non-existent email should fail") {
      for
        runtime <- ZIO.runtime[Any]
        given Runtime[Any] = runtime
        
        authRoutes = AuthRoutes.routes[IO]
        
        loginRequest = Request[IO](Method.POST, uri"/auth/login")
          .withEntity(LoginRequest(
            email = "nonexistent@test.com",
            password = "AnyPass123!"
          ))
        
        loginResponse <- authRoutes.orNotFound.run(loginRequest)
      yield 
        assertTrue(loginResponse.status == Status.Unauthorized)
    }
  ).provide(
    ZLayer.succeed(AppConfig.default),
    DatabaseTestUtils.createTestDatabase,
    UserRepository.layer,
    SecurityService.layer
  ) @@ TestAspect.beforeEach(
    ZIO.serviceWithZIO[doobie.Transactor[Task]](DatabaseTestUtils.cleanDatabase)
  ) @@ TestAspect.sequential

package com.urlshortener.api

import zio.*
import org.http4s.*
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityCodec.*
import com.urlshortener.dto.*
import com.urlshortener.dto.DTOCodecs.given
import com.urlshortener.domain.User
import com.urlshortener.repository.UserRepository
import com.urlshortener.service.SecurityService

object AuthRoutes:
  
  def routes[F[_]: Async](using runtime: Runtime[Any]): HttpRoutes[F] =
    val dsl = Http4sDsl[F]
    import dsl.*
    
    HttpRoutes.of[F] {
      
      // Register
      case req @ POST -> Root / "auth" / "register" =>
        val program = for
          registerReq <- req.as[RegisterRequest]
          securityService <- ZIO.service[SecurityService]
          userRepo <- ZIO.service[UserRepository]
          
          // Validate password strength
          _ <- ZIO.fromEither(securityService.validatePasswordStrength(registerReq.password))
            .mapError(msg => new Exception(msg))
          
          // Check if email exists
          existing <- userRepo.findByEmail(registerReq.email)
          _ <- ZIO.when(existing.isDefined)(
            ZIO.fail(new Exception("Email already registered"))
          )
          
          // Hash password
          hashedPassword <- securityService.hashPassword(registerReq.password)
          
          // Create user
          user = User.create(
            email = registerReq.email,
            hashedPassword = hashedPassword,
            fullName = registerReq.fullName
          )
          savedUser <- userRepo.create(user)
          
          // Generate tokens
          accessToken <- securityService.createAccessToken(savedUser.id, savedUser.email)
          refreshToken <- securityService.createRefreshToken(savedUser.id, savedUser.email)
          
          response = TokenResponse(
            accessToken = accessToken,
            refreshToken = refreshToken
          )
        yield response
        
        Unsafe.unsafe { implicit unsafe =>
          runtime.unsafe.runToFuture(program).flatMap {
            case scala.util.Success(response) => Created(response)
            case scala.util.Failure(ex) => BadRequest(ErrorResponse(ex.getMessage))
          }
        }
      
      // Login
      case req @ POST -> Root / "auth" / "login" =>
        val program = for
          loginReq <- req.as[LoginRequest]
          securityService <- ZIO.service[SecurityService]
          userRepo <- ZIO.service[UserRepository]
          
          // Find user
          user <- userRepo.findByEmail(loginReq.email)
            .someOrFail(new Exception("Invalid email or password"))
          
          // Verify password
          isValid <- securityService.verifyPassword(loginReq.password, user.hashedPassword)
          _ <- ZIO.when(!isValid)(
            ZIO.fail(new Exception("Invalid email or password"))
          )
          
          // Check if active
          _ <- ZIO.when(!user.isActive)(
            ZIO.fail(new Exception("Account is disabled"))
          )
          
          // Update last login
          _ <- userRepo.updateLastLogin(user.id)
          
          // Generate tokens
          accessToken <- securityService.createAccessToken(user.id, user.email)
          refreshToken <- securityService.createRefreshToken(user.id, user.email)
          
          response = TokenResponse(
            accessToken = accessToken,
            refreshToken = refreshToken
          )
        yield response
        
        Unsafe.unsafe { implicit unsafe =>
          runtime.unsafe.runToFuture(program).flatMap {
            case scala.util.Success(response) => Ok(response)
            case scala.util.Failure(ex) => Unauthorized(ErrorResponse(ex.getMessage))
          }
        }
    }

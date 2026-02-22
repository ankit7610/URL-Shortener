package com.urlshortener.service

import zio.*
import com.urlshortener.config.AppConfig
import com.github.t3hnar.bcrypt.*
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}
import io.circe.syntax.*
import io.circe.parser.*
import java.time.Instant
import scala.util.Try

trait SecurityService:
  def hashPassword(password: String): Task[String]
  def verifyPassword(plainPassword: String, hashedPassword: String): Task[Boolean]
  def createAccessToken(userId: Long, email: String): Task[String]
  def createRefreshToken(userId: Long, email: String): Task[String]
  def decodeToken(token: String): Task[Option[(Long, String)]]
  def validatePasswordStrength(password: String): Either[String, Unit]

object SecurityService:
  
  case class Live(config: AppConfig) extends SecurityService:
    
    private val algorithm = JwtAlgorithm.HS256
    
    override def hashPassword(password: String): Task[String] =
      ZIO.attempt(password.bcrypt)
    
    override def verifyPassword(plainPassword: String, hashedPassword: String): Task[Boolean] =
      ZIO.attempt(plainPassword.isBcryptedSafe(hashedPassword).getOrElse(false))
    
    override def createAccessToken(userId: Long, email: String): Task[String] =
      ZIO.attempt {
        val now = Instant.now()
        val expiration = now.plusSeconds(config.jwt.accessTokenExpireMinutes * 60L)
        
        val claim = JwtClaim(
          content = s"""{"userId":$userId,"email":"$email"}""",
          issuedAt = Some(now.getEpochSecond),
          expiration = Some(expiration.getEpochSecond)
        )
        
        JwtCirce.encode(claim, config.jwt.secretKey, algorithm)
      }
    
    override def createRefreshToken(userId: Long, email: String): Task[String] =
      ZIO.attempt {
        val now = Instant.now()
        val expiration = now.plusSeconds(config.jwt.refreshTokenExpireDays * 24L * 60L * 60L)
        
        val claim = JwtClaim(
          content = s"""{"userId":$userId,"email":"$email"}""",
          issuedAt = Some(now.getEpochSecond),
          expiration = Some(expiration.getEpochSecond)
        )
        
        JwtCirce.encode(claim, config.jwt.secretKey, algorithm)
      }
    
    override def decodeToken(token: String): Task[Option[(Long, String)]] =
      ZIO.attempt {
        JwtCirce.decode(token, config.jwt.secretKey, Seq(algorithm)).toOption.flatMap { claim =>
          for
            json <- parse(claim.content).toOption
            cursor = json.hcursor
            userId <- cursor.get[Long]("userId").toOption
            email <- cursor.get[String]("email").toOption
          yield (userId, email)
        }
      }
    
    private val specialChars = "!@#$%^&*()_+-=[]{}|;':"",./<>?"
    
    override def validatePasswordStrength(password: String): Either[String, Unit] =
      if password.length < config.security.passwordMinLength then
        Left(s"Password must be at least ${config.security.passwordMinLength} characters")
      else
        val hasUpper = password.exists(_.isUpper)
        val hasLower = password.exists(_.isLower)
        val hasDigit = password.exists(_.isDigit)
        val hasSpecial = password.exists(specialChars.contains(_))
        
        if !hasUpper || !hasLower || !hasDigit then
          Left("Password must contain uppercase, lowercase, and digit")
        else if !hasSpecial then
          Left("Password must contain at least one special character")
        else
          Right(())
  
  val layer: ZLayer[AppConfig, Nothing, SecurityService] =
    ZLayer.fromFunction(Live.apply)

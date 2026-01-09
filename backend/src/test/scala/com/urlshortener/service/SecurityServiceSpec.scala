package com.urlshortener.service

import zio.*
import zio.test.*
import zio.test.Assertion.*
import com.urlshortener.config.AppConfig

object SecurityServiceSpec extends ZIOSpecDefault:
  
  def spec = suite("SecurityServiceSpec")(
    test("password hashing and verification") {
      for
        service <- ZIO.service[SecurityService]
        password = "SecurePass123!"
        hashed <- service.hashPassword(password)
        isValid <- service.verifyPassword(password, hashed)
        isInvalid <- service.verifyPassword("wrong", hashed)
      yield 
        assertTrue(hashed != password) &&
        assertTrue(isValid) &&
        assertTrue(!isInvalid)
    },
    
    test("JWT token creation and decoding") {
      for
        service <- ZIO.service[SecurityService]
        userId = 123L
        email = "test@example.com"
        token <- service.createAccessToken(userId, email)
        decoded <- service.decodeToken(token)
      yield 
        assertTrue(token.nonEmpty) &&
        assertTrue(decoded == Some((userId, email)))
    },
    
    test("validatePasswordStrength should enforce complexity") {
      for
        service <- ZIO.service[SecurityService]
        res1 = service.validatePasswordStrength("Strong123!")
        res2 = service.validatePasswordStrength("weak")
        res3 = service.validatePasswordStrength("lowercase1")
        res4 = service.validatePasswordStrength("UPPERCASE1")
        res5 = service.validatePasswordStrength("NoDigits!")
      yield 
        assertTrue(res1.isRight) &&
        assertTrue(res2.isLeft) && // too short
        assertTrue(res3.isLeft) && // no upper
        assertTrue(res4.isLeft) && // no lower
        assertTrue(res5.isLeft)    // no digit
    }
  ).provide(
    SecurityService.layer,
    ZLayer.succeed(AppConfig.default)
  )

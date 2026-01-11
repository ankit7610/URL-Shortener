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
    },
    
    test("password hashing should produce different hashes for same password") {
      for
        service <- ZIO.service[SecurityService]
        password = "SamePassword123!"
        hash1 <- service.hashPassword(password)
        hash2 <- service.hashPassword(password)
      yield 
        assertTrue(hash1 != hash2) && // Different salts
        assertTrue(hash1.nonEmpty) &&
        assertTrue(hash2.nonEmpty)
    },
    
    test("refresh token creation and validation") {
      for
        service <- ZIO.service[SecurityService]
        userId = 456L
        email = "refresh@example.com"
        refreshToken <- service.createRefreshToken(userId, email)
        decoded <- service.decodeToken(refreshToken)
      yield 
        assertTrue(refreshToken.nonEmpty) &&
        assertTrue(decoded == Some((userId, email)))
    },
    
    test("decodeToken should handle invalid token") {
      for
        service <- ZIO.service[SecurityService]
        decoded <- service.decodeToken("invalid.token.here")
      yield 
        assertTrue(decoded.isEmpty)
    },
    
    test("decodeToken should handle malformed token") {
      for
        service <- ZIO.service[SecurityService]
        decoded <- service.decodeToken("not-even-a-jwt")
      yield 
        assertTrue(decoded.isEmpty)
    },
    
    test("validatePasswordStrength should accept passwords with special characters") {
      for
        service <- ZIO.service[SecurityService]
        res1 = service.validatePasswordStrength("P@ssw0rd!")
        res2 = service.validatePasswordStrength("C0mpl3x#Pass")
        res3 = service.validatePasswordStrength("Str0ng$ecure")
      yield 
        assertTrue(res1.isRight) &&
        assertTrue(res2.isRight) &&
        assertTrue(res3.isRight)
    },
    
    test("validatePasswordStrength should handle very long passwords") {
      for
        service <- ZIO.service[SecurityService]
        longPassword = "VeryLong123!" + ("a" * 100)
        res = service.validatePasswordStrength(longPassword)
      yield 
        assertTrue(res.isRight)
    },
    
    test("validatePasswordStrength should reject password without uppercase") {
      for
        service <- ZIO.service[SecurityService]
        res = service.validatePasswordStrength("alllowercase123")
      yield 
        assertTrue(res.isLeft)
    },
    
    test("validatePasswordStrength should reject password without lowercase") {
      for
        service <- ZIO.service[SecurityService]
        res = service.validatePasswordStrength("ALLUPPERCASE123")
      yield 
        assertTrue(res.isLeft)
    },
    
    test("validatePasswordStrength should reject password without digit") {
      for
        service <- ZIO.service[SecurityService]
        res = service.validatePasswordStrength("NoDigitsHere!")
      yield 
        assertTrue(res.isLeft)
    }
  ).provide(
    SecurityService.layer,
    ZLayer.succeed(AppConfig.default)
  )

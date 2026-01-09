package com.urlshortener.service

import zio.*
import zio.test.*
import zio.test.Assertion.*
import com.urlshortener.config.AppConfig

object URLShortenerServiceSpec extends ZIOSpecDefault:
  
  def spec = suite("URLShortenerServiceSpec")(
    test("encode should convert number to base62 string") {
      for
        service <- ZIO.service[URLShortenerService]
        res1 = service.encode(0L)
        res2 = service.encode(61L)
        res3 = service.encode(62L)
        res4 = service.encode(1000L)
      yield 
        assertTrue(res1 == "0") &&
        assertTrue(res2 == "Z") &&
        assertTrue(res3 == "10") &&
        assertTrue(res4 == "G8")
    },
    
    test("decode should convert base62 string back to number") {
      for
        service <- ZIO.service[URLShortenerService]
        res1 = service.decode("0")
        res2 = service.decode("Z")
        res3 = service.decode("10")
        res4 = service.decode("G8")
      yield 
        assertTrue(res1 == Some(0L)) &&
        assertTrue(res2 == Some(61L)) &&
        assertTrue(res3 == Some(62L)) &&
        assertTrue(res4 == Some(1000L))
    },
    
    test("generateShortCode should handle padding") {
      for
        service <- ZIO.service[URLShortenerService]
        code = service.generateShortCode(1L)
      yield 
        assertTrue(code.length == 7) &&
        assertTrue(code.endsWith("1"))
    },
    
    test("validateUrl should accept valid http/https URLs") {
      for
        service <- ZIO.service[URLShortenerService]
        res1 = service.validateUrl("https://google.com")
        res2 = service.validateUrl("http://example.org/path?q=1")
      yield 
        assertTrue(res1.isRight) &&
        assertTrue(res2.isRight)
    },
    
    test("validateUrl should reject invalid protocols") {
      for
        service <- ZIO.service[URLShortenerService]
        res1 = service.validateUrl("javascript:alert(1)")
        res2 = service.validateUrl("ftp://files.com")
        res3 = service.validateUrl("data:text/plain;base64,SGVsbG8=")
      yield 
        assertTrue(res1.isLeft) &&
        assertTrue(res2.isLeft) &&
        assertTrue(res3.isLeft)
    },
    
    test("validateCustomAlias should enforce constraints") {
      for
        service <- ZIO.service[URLShortenerService]
        res1 = service.validateCustomAlias("ok-alias_123")
        res2 = service.validateCustomAlias("sh") // too short
        res3 = service.validateCustomAlias("api") // reserved
        res4 = service.validateCustomAlias("invalid!") // invalid char
      yield 
        assertTrue(res1.isRight) &&
        assertTrue(res2.isLeft) &&
        assertTrue(res3.isLeft) &&
        assertTrue(res4.isLeft)
    }
  ).provide(
    URLShortenerService.layer,
    ZLayer.succeed(AppConfig.default)
  )

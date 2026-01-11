package com.urlshortener.service

import zio.*
import zio.test.*
import zio.test.Assertion.*

object QRCodeServiceSpec extends ZIOSpecDefault:
  
  def spec = suite("QRCodeServiceSpec")(
    test("generateQRCode should return non-empty byte array") {
      for
        service <- ZIO.service[QRCodeService]
        qrBytes <- service.generateQRCode("https://example.com", size = 200)
      yield 
        assertTrue(qrBytes.nonEmpty) &&
        assertTrue(qrBytes.length > 100) // Basic sanity check for PNG header/data
    },
    
    test("generateQRCode should handle different sizes") {
      for
        service <- ZIO.service[QRCodeService]
        qrBytesSmall <- service.generateQRCode("https://example.com", size = 100)
        qrBytesLarge <- service.generateQRCode("https://example.com", size = 500)
      yield 
        assertTrue(qrBytesLarge.length > qrBytesSmall.length)
    },
    
    test("generateQRCode should handle very long URLs") {
      for
        service <- ZIO.service[QRCodeService]
        longUrl = "https://example.com/path?" + ("param=value&" * 100)
        qrBytes <- service.generateQRCode(longUrl, size = 300)
      yield 
        assertTrue(qrBytes.nonEmpty) &&
        assertTrue(qrBytes.length > 100)
    },
    
    test("generateQRCode should handle extreme sizes") {
      for
        service <- ZIO.service[QRCodeService]
        qrBytesMin <- service.generateQRCode("https://example.com", size = 50)
        qrBytesMax <- service.generateQRCode("https://example.com", size = 1000)
      yield 
        assertTrue(qrBytesMin.nonEmpty) &&
        assertTrue(qrBytesMax.nonEmpty) &&
        assertTrue(qrBytesMax.length > qrBytesMin.length)
    },
    
    test("generateQRCode should handle different border sizes") {
      for
        service <- ZIO.service[QRCodeService]
        qrNoBorder <- service.generateQRCode("https://example.com", size = 200, border = 0)
        qrSmallBorder <- service.generateQRCode("https://example.com", size = 200, border = 1)
        qrLargeBorder <- service.generateQRCode("https://example.com", size = 200, border = 10)
      yield 
        assertTrue(qrNoBorder.nonEmpty) &&
        assertTrue(qrSmallBorder.nonEmpty) &&
        assertTrue(qrLargeBorder.nonEmpty)
    },
    
    test("generateQRCode should handle URLs with special characters") {
      for
        service <- ZIO.service[QRCodeService]
        urlWithSpecialChars = "https://example.com/path?q=hello%20world&foo=bar#section"
        qrBytes <- service.generateQRCode(urlWithSpecialChars, size = 200)
      yield 
        assertTrue(qrBytes.nonEmpty) &&
        assertTrue(qrBytes.length > 100)
    }

  ).provide(QRCodeService.layer)

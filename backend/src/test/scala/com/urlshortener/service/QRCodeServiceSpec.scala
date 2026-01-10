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
    }
  ).provide(QRCodeService.layer)

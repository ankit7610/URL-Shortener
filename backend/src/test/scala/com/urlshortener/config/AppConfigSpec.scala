package com.urlshortener.config

import zio.*
import zio.test.*
import zio.test.Assertion.*

object AppConfigSpec extends ZIOSpecDefault:
  
  def spec = suite("AppConfigSpec")(
    test("load default configuration") {
      val config = AppConfig.default
      assertTrue(config.appName == "URL Shortener") &&
      assertTrue(config.environment == "development") &&
      assertTrue(config.debug == true)
    },
    
    test("isProduction and isDevelopment helpers") {
      val devConfig = AppConfig.default.copy(environment = "development")
      val prodConfig = AppConfig.default.copy(environment = "production")
      
      assertTrue(devConfig.isDevelopment) &&
      assertTrue(!devConfig.isProduction) &&
      assertTrue(prodConfig.isProduction) &&
      assertTrue(!prodConfig.isDevelopment)
    }
  )

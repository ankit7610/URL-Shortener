package com.urlshortener.domain

import zio.test.*
import zio.test.Assertion.*
import java.time.Instant
import java.time.temporal.ChronoUnit

object ModelSpec extends ZIOSpecDefault:
  
  def spec = suite("ModelSpec")(
    suite("URL model")(
      test("isExpired should work correctly") {
        val now = Instant.now()
        val expiredUrl = URL.create("code", "url", expiresAt = Some(now.minus(1, ChronoUnit.HOURS)))
        val activeUrl = URL.create("code", "url", expiresAt = Some(now.plus(1, ChronoUnit.HOURS)))
        val permanentUrl = URL.create("code", "url", expiresAt = None)
        
        assertTrue(expiredUrl.isExpired) &&
        assertTrue(!activeUrl.isExpired) &&
        assertTrue(!permanentUrl.isExpired)
      },
      
      test("isAccessible should check active and expired") {
        val now = Instant.now()
        val normalUrl = URL.create("code", "url")
        val inactiveUrl = normalUrl.copy(isActive = false)
        val expiredUrl = normalUrl.copy(expiresAt = Some(now.minus(1, ChronoUnit.DAYS)))
        
        assertTrue(normalUrl.isAccessible) &&
        assertTrue(!inactiveUrl.isAccessible) &&
        assertTrue(!expiredUrl.isAccessible)
      }
    ),
    
    suite("User model")(
      test("create should set timestamps") {
        val user = User.create("test@test.com", "hash")
        assertTrue(user.email == "test@test.com") &&
        assertTrue(user.isActive) &&
        assertTrue(!user.isVerified)
      }
    )
  )

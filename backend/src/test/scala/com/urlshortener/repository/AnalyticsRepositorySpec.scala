package com.urlshortener.repository

import zio.*
import zio.test.*
import zio.test.Assertion.*
import doobie.*
import com.urlshortener.domain.{Analytics, URL => DomainURL}
import java.time.Instant

object AnalyticsRepositorySpec extends ZIOSpecDefault:
  
  def spec = suite("AnalyticsRepositorySpec")(
    test("create should insert analytics record") {
      for
        analyticsRepo <- ZIO.service[AnalyticsRepository]
        urlRepo <- ZIO.service[URLRepository]
        
        // Create a URL first
        url = DomainURL.create("test1", "https://example.com")
        createdUrl <- urlRepo.create(url)
        
        // Create analytics
        analytics = Analytics.create(
          urlId = createdUrl.id,
          ipAddress = Some("192.168.1.1"),
          userAgent = Some("Mozilla/5.0"),
          referrer = Some("https://google.com")
        ).copy(
          country = Some("US"),
          city = Some("New York"),
          deviceType = Some("Desktop"),
          browser = Some("Chrome"),
          os = Some("Windows")
        )
        
        created <- analyticsRepo.create(analytics)
      yield 
        assertTrue(created.id > 0) &&
        assertTrue(created.urlId == createdUrl.id) &&
        assertTrue(created.ipAddress == Some("192.168.1.1"))
    },
    
    test("findByUrlId should return analytics for URL") {
      for
        analyticsRepo <- ZIO.service[AnalyticsRepository]
        urlRepo <- ZIO.service[URLRepository]
        
        url = DomainURL.create("test2", "https://example.com")
        createdUrl <- urlRepo.create(url)
        
        // Create multiple analytics
        _ <- analyticsRepo.create(Analytics.create(createdUrl.id, Some("1.1.1.1")))
        _ <- analyticsRepo.create(Analytics.create(createdUrl.id, Some("2.2.2.2")))
        
        records <- analyticsRepo.findByUrlId(createdUrl.id, days = 30)
      yield 
        assertTrue(records.length == 2)
    },
    
    test("getClickStats should aggregate statistics") {
      for
        analyticsRepo <- ZIO.service[AnalyticsRepository]
        urlRepo <- ZIO.service[URLRepository]
        
        url = DomainURL.create("stats1", "https://example.com")
        createdUrl <- urlRepo.create(url)
        
        // Create analytics with different countries and devices
        _ <- analyticsRepo.create(
          Analytics.create(createdUrl.id, Some("1.1.1.1"))
            .copy(country = Some("US"), deviceType = Some("Desktop"), browser = Some("Chrome"))
        )
        _ <- analyticsRepo.create(
          Analytics.create(createdUrl.id, Some("2.2.2.2"))
            .copy(country = Some("US"), deviceType = Some("Mobile"), browser = Some("Safari"))
        )
        _ <- analyticsRepo.create(
          Analytics.create(createdUrl.id, Some("3.3.3.3"))
            .copy(country = Some("UK"), deviceType = Some("Desktop"), browser = Some("Chrome"))
        )
        
        stats <- analyticsRepo.getClickStats(createdUrl.id, days = 30)
      yield 
        assertTrue(stats.totalClicks == 3) &&
        assertTrue(stats.uniqueVisitors == 3) &&
        assertTrue(stats.topCountries.exists(_._1 == "US")) &&
        assertTrue(stats.topDevices.exists(_._1 == "Desktop")) &&
        assertTrue(stats.topBrowsers.exists(_._1 == "Chrome"))
    },
    
    test("getClickStats should count unique visitors correctly") {
      for
        analyticsRepo <- ZIO.service[AnalyticsRepository]
        urlRepo <- ZIO.service[URLRepository]
        
        url = DomainURL.create("unique1", "https://example.com")
        createdUrl <- urlRepo.create(url)
        
        // Same IP multiple times
        _ <- analyticsRepo.create(Analytics.create(createdUrl.id, Some("1.1.1.1")))
        _ <- analyticsRepo.create(Analytics.create(createdUrl.id, Some("1.1.1.1")))
        _ <- analyticsRepo.create(Analytics.create(createdUrl.id, Some("2.2.2.2")))
        
        stats <- analyticsRepo.getClickStats(createdUrl.id, days = 30)
      yield 
        assertTrue(stats.totalClicks == 3) &&
        assertTrue(stats.uniqueVisitors == 2) // Only 2 unique IPs
    },
    
    test("getClickStats should filter by days") {
      for
        analyticsRepo <- ZIO.service[AnalyticsRepository]
        urlRepo <- ZIO.service[URLRepository]
        
        url = DomainURL.create("days1", "https://example.com")
        createdUrl <- urlRepo.create(url)
        
        // Create analytics
        _ <- analyticsRepo.create(Analytics.create(createdUrl.id, Some("1.1.1.1")))
        
        stats7 <- analyticsRepo.getClickStats(createdUrl.id, days = 7)
        stats30 <- analyticsRepo.getClickStats(createdUrl.id, days = 30)
      yield 
        assertTrue(stats7.totalClicks == 1) &&
        assertTrue(stats30.totalClicks == 1)
    },
    
    test("getClickStats should group clicks by day") {
      for
        analyticsRepo <- ZIO.service[AnalyticsRepository]
        urlRepo <- ZIO.service[URLRepository]
        
        url = DomainURL.create("byday1", "https://example.com")
        createdUrl <- urlRepo.create(url)
        
        _ <- analyticsRepo.create(Analytics.create(createdUrl.id, Some("1.1.1.1")))
        _ <- analyticsRepo.create(Analytics.create(createdUrl.id, Some("2.2.2.2")))
        
        stats <- analyticsRepo.getClickStats(createdUrl.id, days = 30)
      yield 
        assertTrue(stats.clicksByDay.nonEmpty)
    }
  ).provide(
    DatabaseTestUtils.createTestDatabase,
    URLRepository.layer,
    AnalyticsRepository.layer
  ) @@ TestAspect.beforeEach(
    ZIO.serviceWithZIO[Transactor[Task]](DatabaseTestUtils.cleanDatabase)
  ) @@ TestAspect.sequential

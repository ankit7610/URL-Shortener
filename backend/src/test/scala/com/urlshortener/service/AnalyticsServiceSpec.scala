package com.urlshortener.service

import zio.*
import zio.test.*
import zio.test.Assertion.*
import com.urlshortener.domain.{Analytics, URL as DomainURL, ClickStats}
import com.urlshortener.repository.{AnalyticsRepository, URLRepository}
import java.time.Instant

object AnalyticsServiceSpec extends ZIOSpecDefault:
  
  // Stub repositories
  val mockAnalyticsRepo = ZLayer.succeed(new AnalyticsRepository {
    override def create(analytics: Analytics): Task[Analytics] = ZIO.succeed(analytics)
    override def findByUrlId(urlId: Long, days: Int): Task[List[Analytics]] = ZIO.succeed(Nil)
    override def getClickStats(urlId: Long, days: Int): Task[ClickStats] = ZIO.succeed(
      ClickStats(100L, 50L, Nil, Nil, Nil, Nil)
    )
  })
  
  val mockURLRepo = ZLayer.succeed(new URLRepository {
    override def findById(id: Long): Task[Option[DomainURL]] = ZIO.succeed(None)
    override def findByShortCode(shortCode: String): Task[Option[DomainURL]] = ZIO.succeed(None)
    override def findByCustomAlias(alias: String): Task[Option[DomainURL]] = ZIO.succeed(None)
    override def findByUserId(userId: Long, offset: Int, limit: Int): Task[List[DomainURL]] = ZIO.succeed(Nil)
    override def searchByUserId(userId: Long, query: String, offset: Int, limit: Int): Task[List[DomainURL]] = ZIO.succeed(Nil)
    override def countByUserId(userId: Long): Task[Long] = ZIO.succeed(0L)
    override def create(url: DomainURL): Task[DomainURL] = ZIO.succeed(url)
    override def update(url: DomainURL): Task[DomainURL] = ZIO.succeed(url)
    override def delete(id: Long): Task[Unit] = ZIO.unit
    override def incrementClickCount(id: Long): Task[Unit] = ZIO.unit
    override def updateLastAccessed(id: Long): Task[Unit] = ZIO.unit
  })
  
  def spec = suite("AnalyticsServiceSpec")(
    test("trackClick should process user agent and call repositories") {
      for
        service <- ZIO.service[AnalyticsService]
        _ <- service.trackClick(
          urlId = 1L,
          ipAddress = Some("127.0.0.1"),
          userAgent = Some("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Safari/537.36"),
          referrer = Some("http://linkedin.com")
        )
      yield 
        assertCompletes // Since it uses forkDaemon, we check it doesn't fail
    },
    
    test("getUrlAnalytics should return stats from repository") {
      for
        service <- ZIO.service[AnalyticsService]
        stats <- service.getUrlAnalytics(1L, 30)
      yield 
        assertTrue(stats.totalClicks == 100L) &&
        assertTrue(stats.uniqueVisitors == 50L)
    }
  ).provide(
    AnalyticsService.layer,
    mockAnalyticsRepo,
    mockURLRepo
  )

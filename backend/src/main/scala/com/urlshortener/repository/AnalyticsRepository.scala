package com.urlshortener.repository

import cats.effect.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import zio.*
import zio.interop.catz.*
import com.urlshortener.domain.{Analytics, ClickStats}
import java.time.Instant
import java.time.temporal.ChronoUnit

trait AnalyticsRepository:
  def create(analytics: Analytics): Task[Analytics]
  def findByUrlId(urlId: Long, days: Int): Task[List[Analytics]]
  def getClickStats(urlId: Long, days: Int): Task[ClickStats]

object AnalyticsRepository:
  
  case class Live(xa: Transactor[Task]) extends AnalyticsRepository:
    
    override def create(analytics: Analytics): Task[Analytics] =
      sql"""
        INSERT INTO analytics (url_id, clicked_at, ip_address, country, city,
                              device_type, browser, os, referrer, user_agent)
        VALUES (${analytics.urlId}, ${analytics.clickedAt}, ${analytics.ipAddress},
                ${analytics.country}, ${analytics.city}, ${analytics.deviceType},
                ${analytics.browser}, ${analytics.os}, ${analytics.referrer},
                ${analytics.userAgent})
        RETURNING id, url_id, clicked_at, ip_address, country, city,
                  device_type, browser, os, referrer, user_agent
      """.query[Analytics].unique.transact(xa)
    
    override def findByUrlId(urlId: Long, days: Int): Task[List[Analytics]] =
      val since = Instant.now().minus(days.toLong, ChronoUnit.DAYS)
      sql"""
        SELECT id, url_id, clicked_at, ip_address, country, city,
               device_type, browser, os, referrer, user_agent
        FROM analytics
        WHERE url_id = $urlId AND clicked_at >= $since
        ORDER BY clicked_at DESC
      """.query[Analytics].to[List].transact(xa)
    
    override def getClickStats(urlId: Long, days: Int): Task[ClickStats] =
      val since = Instant.now().minus(days.toLong, ChronoUnit.DAYS)
      
      for
        totalClicks <- sql"""
          SELECT COUNT(*) FROM analytics
          WHERE url_id = $urlId AND clicked_at >= $since
        """.query[Long].unique.transact(xa)
        
        uniqueVisitors <- sql"""
          SELECT COUNT(DISTINCT ip_address) FROM analytics
          WHERE url_id = $urlId AND clicked_at >= $since AND ip_address IS NOT NULL
        """.query[Long].unique.transact(xa)
        
        topCountries <- sql"""
          SELECT country, COUNT(*) as count
          FROM analytics
          WHERE url_id = $urlId AND clicked_at >= $since AND country IS NOT NULL
          GROUP BY country
          ORDER BY count DESC
          LIMIT 10
        """.query[(String, Long)].to[List].transact(xa)
        
        topDevices <- sql"""
          SELECT device_type, COUNT(*) as count
          FROM analytics
          WHERE url_id = $urlId AND clicked_at >= $since AND device_type IS NOT NULL
          GROUP BY device_type
          ORDER BY count DESC
          LIMIT 10
        """.query[(String, Long)].to[List].transact(xa)
        
        topBrowsers <- sql"""
          SELECT browser, COUNT(*) as count
          FROM analytics
          WHERE url_id = $urlId AND clicked_at >= $since AND browser IS NOT NULL
          GROUP BY browser
          ORDER BY count DESC
          LIMIT 10
        """.query[(String, Long)].to[List].transact(xa)
        
        clicksByDay <- sql"""
          SELECT DATE(clicked_at)::text, COUNT(*) as count
          FROM analytics
          WHERE url_id = $urlId AND clicked_at >= $since
          GROUP BY DATE(clicked_at)
          ORDER BY DATE(clicked_at) DESC
        """.query[(String, Long)].to[List].transact(xa)
      yield ClickStats(
        totalClicks = totalClicks,
        uniqueVisitors = uniqueVisitors,
        topCountries = topCountries,
        topDevices = topDevices,
        topBrowsers = topBrowsers,
        clicksByDay = clicksByDay
      )
  
  val layer: ZLayer[Transactor[Task], Nothing, AnalyticsRepository] =
    ZLayer.fromFunction(Live.apply)

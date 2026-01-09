package com.urlshortener.service

import zio.*
import com.urlshortener.domain.Analytics
import com.urlshortener.repository.{AnalyticsRepository, URLRepository}
import eu.bitwalker.useragentutils.UserAgent
import java.time.Instant

trait AnalyticsService:
  def trackClick(
    urlId: Long,
    ipAddress: Option[String],
    userAgent: Option[String],
    referrer: Option[String]
  ): Task[Unit]
  
  def getUrlAnalytics(urlId: Long, days: Int): Task[com.urlshortener.domain.ClickStats]

object AnalyticsService:
  
  case class Live(
    analyticsRepo: AnalyticsRepository,
    urlRepo: URLRepository
  ) extends AnalyticsService:
    
    override def trackClick(
      urlId: Long,
      ipAddress: Option[String],
      userAgent: Option[String],
      referrer: Option[String]
    ): Task[Unit] =
      for
        // Parse user agent
        parsedUA = userAgent.flatMap(ua => scala.util.Try(UserAgent.parseUserAgentString(ua)).toOption)
        deviceType = parsedUA.map(_.getOperatingSystem.getDeviceType.getName)
        browser = parsedUA.map(_.getBrowser.getName)
        os = parsedUA.map(_.getOperatingSystem.getName)
        
        // Create analytics record
        analytics = Analytics.create(
          urlId = urlId,
          ipAddress = ipAddress,
          userAgent = userAgent,
          referrer = referrer
        ).copy(
          deviceType = deviceType,
          browser = browser,
          os = os
        )
        
        // Save analytics (async, don't block)
        _ <- analyticsRepo.create(analytics).forkDaemon
        
        // Update click count and last accessed
        _ <- urlRepo.incrementClickCount(urlId).forkDaemon
        _ <- urlRepo.updateLastAccessed(urlId).forkDaemon
      yield ()
    
    override def getUrlAnalytics(urlId: Long, days: Int): Task[com.urlshortener.domain.ClickStats] =
      analyticsRepo.getClickStats(urlId, days)
  
  val layer: ZLayer[AnalyticsRepository & URLRepository, Nothing, AnalyticsService] =
    ZLayer.fromFunction(Live.apply)

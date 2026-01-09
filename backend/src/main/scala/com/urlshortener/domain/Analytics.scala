package com.urlshortener.domain

import java.time.Instant

case class Analytics(
  id: Long,
  urlId: Long,
  clickedAt: Instant,
  ipAddress: Option[String],
  country: Option[String],
  city: Option[String],
  deviceType: Option[String],
  browser: Option[String],
  os: Option[String],
  referrer: Option[String],
  userAgent: Option[String]
)

object Analytics:
  def create(
    urlId: Long,
    ipAddress: Option[String] = None,
    userAgent: Option[String] = None,
    referrer: Option[String] = None
  ): Analytics =
    Analytics(
      id = 0, // Will be set by database
      urlId = urlId,
      clickedAt = Instant.now(),
      ipAddress = ipAddress,
      country = None, // Will be enriched by geolocation service
      city = None,
      deviceType = None, // Will be parsed from user agent
      browser = None,
      os = None,
      referrer = referrer,
      userAgent = userAgent
    )

case class ClickStats(
  totalClicks: Long,
  uniqueVisitors: Long,
  topCountries: List[(String, Long)],
  topDevices: List[(String, Long)],
  topBrowsers: List[(String, Long)],
  clicksByDay: List[(String, Long)]
)

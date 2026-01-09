package com.urlshortener.domain

import java.time.Instant

case class URL(
  id: Long,
  shortCode: String,
  originalUrl: String,
  customAlias: Option[String],
  title: Option[String],
  description: Option[String],
  userId: Option[Long],
  passwordHash: Option[String],
  isActive: Boolean,
  expiresAt: Option[Instant],
  clickCount: Long,
  createdAt: Instant,
  updatedAt: Instant,
  lastAccessed: Option[Instant]
):
  def isExpired: Boolean =
    expiresAt.exists(_.isBefore(Instant.now()))
  
  def isAccessible: Boolean =
    isActive && !isExpired
  
  def isPasswordProtected: Boolean =
    passwordHash.isDefined

object URL:
  def create(
    shortCode: String,
    originalUrl: String,
    customAlias: Option[String] = None,
    title: Option[String] = None,
    description: Option[String] = None,
    userId: Option[Long] = None,
    passwordHash: Option[String] = None,
    expiresAt: Option[Instant] = None
  ): URL =
    val now = Instant.now()
    URL(
      id = 0, // Will be set by database
      shortCode = shortCode,
      originalUrl = originalUrl,
      customAlias = customAlias,
      title = title,
      description = description,
      userId = userId,
      passwordHash = passwordHash,
      isActive = true,
      expiresAt = expiresAt,
      clickCount = 0,
      createdAt = now,
      updatedAt = now,
      lastAccessed = None
    )

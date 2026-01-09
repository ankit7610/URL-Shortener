package com.urlshortener.domain

import java.time.Instant

case class User(
  id: Long,
  email: String,
  hashedPassword: String,
  fullName: Option[String],
  isActive: Boolean,
  isVerified: Boolean,
  isAdmin: Boolean,
  apiKey: Option[String],
  createdAt: Instant,
  updatedAt: Instant,
  lastLogin: Option[Instant]
)

object User:
  def create(
    email: String,
    hashedPassword: String,
    fullName: Option[String] = None
  ): User =
    val now = Instant.now()
    User(
      id = 0, // Will be set by database
      email = email,
      hashedPassword = hashedPassword,
      fullName = fullName,
      isActive = true,
      isVerified = false,
      isAdmin = false,
      apiKey = None,
      createdAt = now,
      updatedAt = now,
      lastLogin = None
    )

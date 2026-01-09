package com.urlshortener.dto

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.*
import java.time.Instant

// Request DTOs
case class RegisterRequest(
  email: String,
  password: String,
  fullName: Option[String]
)

case class LoginRequest(
  email: String,
  password: String
)

case class URLCreateRequest(
  originalUrl: String,
  customAlias: Option[String] = None,
  title: Option[String] = None,
  description: Option[String] = None,
  password: Option[String] = None,
  expiresInDays: Option[Int] = None
)

case class URLUpdateRequest(
  originalUrl: Option[String] = None,
  title: Option[String] = None,
  description: Option[String] = None,
  isActive: Option[Boolean] = None,
  expiresInDays: Option[Int] = None
)

// Response DTOs
case class TokenResponse(
  accessToken: String,
  refreshToken: String,
  tokenType: String = "Bearer"
)

case class UserResponse(
  id: Long,
  email: String,
  fullName: Option[String],
  isActive: Boolean,
  isVerified: Boolean,
  isAdmin: Boolean,
  createdAt: Instant
)

case class URLResponse(
  id: Long,
  shortCode: String,
  originalUrl: String,
  shortUrl: String,
  customAlias: Option[String],
  title: Option[String],
  description: Option[String],
  isPasswordProtected: Boolean,
  isActive: Boolean,
  expiresAt: Option[Instant],
  clickCount: Long,
  createdAt: Instant,
  qrCodeUrl: String
)

case class URLListResponse(
  urls: List[URLResponse],
  total: Long,
  page: Int,
  pageSize: Int,
  totalPages: Int
)

case class AnalyticsSummary(
  totalClicks: Long,
  uniqueVisitors: Long,
  topCountries: List[(String, Long)],
  topDevices: List[(String, Long)],
  topBrowsers: List[(String, Long)],
  clicksByDay: List[(String, Long)]
)

case class HealthResponse(
  status: String,
  version: String,
  environment: String,
  database: String,
  cache: String
)

case class MessageResponse(
  message: String
)

case class ErrorResponse(
  error: String,
  details: Option[String] = None
)

// Circe codecs
object DTOCodecs:
  given Decoder[RegisterRequest] = deriveDecoder
  given Encoder[RegisterRequest] = deriveEncoder
  
  given Decoder[LoginRequest] = deriveDecoder
  given Encoder[LoginRequest] = deriveEncoder
  
  given Decoder[URLCreateRequest] = deriveDecoder
  given Encoder[URLCreateRequest] = deriveEncoder
  
  given Decoder[URLUpdateRequest] = deriveDecoder
  given Encoder[URLUpdateRequest] = deriveEncoder
  
  given Decoder[TokenResponse] = deriveDecoder
  given Encoder[TokenResponse] = deriveEncoder
  
  given Decoder[UserResponse] = deriveDecoder
  given Encoder[UserResponse] = deriveEncoder
  
  given Decoder[URLResponse] = deriveDecoder
  given Encoder[URLResponse] = deriveEncoder
  
  given Decoder[URLListResponse] = deriveDecoder
  given Encoder[URLListResponse] = deriveEncoder
  
  given Decoder[AnalyticsSummary] = deriveDecoder
  given Encoder[AnalyticsSummary] = deriveEncoder
  
  given Decoder[HealthResponse] = deriveDecoder
  given Encoder[HealthResponse] = deriveEncoder
  
  given Decoder[MessageResponse] = deriveDecoder
  given Encoder[MessageResponse] = deriveEncoder
  
  given Decoder[ErrorResponse] = deriveDecoder
  given Encoder[ErrorResponse] = deriveEncoder

package com.urlshortener.service

import zio.*
import com.urlshortener.config.AppConfig
import java.net.{InetAddress, URI, URL as JavaURL}
import scala.util.Try

trait URLShortenerService:
  def encode(num: Long): String
  def decode(shortCode: String): Option[Long]
  def generateShortCode(urlId: Long, retryCount: Int = 0): String
  def validateCustomAlias(alias: String): Either[String, Unit]
  def validateUrl(url: String): Either[String, Unit]

object URLShortenerService:
  
  case class Live(config: AppConfig) extends URLShortenerService:
    
    private val alphabet = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private val base = alphabet.length
    
    private val maxUrlLength = 2048
    
    private val maliciousPatterns = List(
      "javascript:",
      "data:",
      "vbscript:",
      "file:"
    )
    
    private val privateIpPatterns = List(
      "^127\\.",
      "^10\\.",
      "^192\\.168\\.",
      "^172\\.(1[6-9]|2[0-9]|3[0-1])\\.",
      "^0\\.0\\.0\\.0",
      "^localhost$"
    ).map(_.r)
    
    override def encode(num: Long): String =
      if num == 0 then alphabet.charAt(0).toString
      else
        var n = num
        val result = new StringBuilder
        while n > 0 do
          val remainder = (n % base).toInt
          result.append(alphabet.charAt(remainder))
          n = n / base
        result.reverse.toString
    
    override def decode(shortCode: String): Option[Long] =
      Try {
        shortCode.foldLeft(0L) { (acc, char) =>
          acc * base + alphabet.indexOf(char)
        }
      }.toOption.filter(_ >= 0)
    
    override def generateShortCode(urlId: Long, retryCount: Int = 0): String =
      val adjustedId = urlId + (retryCount * 1000000L)
      val code = encode(adjustedId)
      
      // Ensure minimum length by padding with random characters
      if code.length < config.url.shortUrlLength then
        val paddingLength = config.url.shortUrlLength - code.length
        val padding = (1 to paddingLength).map(_ => alphabet.charAt(scala.util.Random.nextInt(base))).mkString
        padding + code
      else
        code
    
    override def validateCustomAlias(alias: String): Either[String, Unit] =
      // Check length
      if alias.length < config.url.customAliasMinLength then
        Left(s"Alias must be at least ${config.url.customAliasMinLength} characters")
      else if alias.length > config.url.customAliasMaxLength then
        Left(s"Alias must be at most ${config.url.customAliasMaxLength} characters")
      // Check characters (alphanumeric, dash, underscore only)
      else if !alias.forall(c => alphabet.contains(c) || c == '-' || c == '_') then
        Left("Alias can only contain letters, numbers, dashes, and underscores")
      // Check for reserved words
      else if Set("api", "admin", "dashboard", "health", "metrics", "docs", "swagger").contains(alias.toLowerCase) then
        Left("This alias is reserved")
      else
        Right(())
    
    private def isPrivateIp(host: String): Boolean =
      Try {
        val addr = InetAddress.getByName(host)
        addr.isLoopbackAddress || addr.isSiteLocalAddress || addr.isLinkLocalAddress ||
          privateIpPatterns.exists(_.findFirstIn(host).isDefined)
      }.getOrElse(false)
    
    override def validateUrl(url: String): Either[String, Unit] =
      // Check URL length
      if url.length > maxUrlLength then
        Left(s"URL must be at most $maxUrlLength characters")
      else
        // Trim and check for malicious patterns
        val urlLower = url.trim.toLowerCase
        maliciousPatterns.find(urlLower.startsWith) match
          case Some(pattern) => Left("URL contains potentially malicious protocol")
          case None =>
            // Parse URL
            Try {
              val uri = new URI(url)
              val scheme = Option(uri.getScheme)
              val host = Option(uri.getHost)
              
              if scheme.isEmpty || host.isEmpty then
                Left("Invalid URL format")
              else if !Set("http", "https").contains(scheme.get.toLowerCase) then
                Left("Only HTTP and HTTPS URLs are allowed")
              else if config.isProduction && isPrivateIp(host.get) then
                Left("URLs pointing to private/internal networks are not allowed")
              else
                Right(())
            }.toEither.left.map(_ => "Invalid URL format").flatten
  
  val layer: ZLayer[AppConfig, Nothing, URLShortenerService] =
    ZLayer.fromFunction(Live.apply)

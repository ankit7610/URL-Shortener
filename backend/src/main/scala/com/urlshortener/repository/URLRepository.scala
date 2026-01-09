package com.urlshortener.repository

import cats.effect.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import zio.*
import zio.interop.catz.*
import com.urlshortener.domain.URL
import java.time.Instant

trait URLRepository:
  def findById(id: Long): Task[Option[URL]]
  def findByShortCode(shortCode: String): Task[Option[URL]]
  def findByCustomAlias(alias: String): Task[Option[URL]]
  def findByUserId(userId: Long, offset: Int, limit: Int): Task[List[URL]]
  def searchByUserId(userId: Long, query: String, offset: Int, limit: Int): Task[List[URL]]
  def countByUserId(userId: Long): Task[Long]
  def create(url: URL): Task[URL]
  def update(url: URL): Task[URL]
  def delete(id: Long): Task[Unit]
  def incrementClickCount(id: Long): Task[Unit]
  def updateLastAccessed(id: Long): Task[Unit]

object URLRepository:
  
  case class Live(xa: Transactor[Task]) extends URLRepository:
    
    override def findById(id: Long): Task[Option[URL]] =
      sql"""
        SELECT id, short_code, original_url, custom_alias, title, description,
               user_id, password_hash, is_active, expires_at, click_count,
               created_at, updated_at, last_accessed
        FROM urls
        WHERE id = $id
      """.query[URL].option.transact(xa)
    
    override def findByShortCode(shortCode: String): Task[Option[URL]] =
      sql"""
        SELECT id, short_code, original_url, custom_alias, title, description,
               user_id, password_hash, is_active, expires_at, click_count,
               created_at, updated_at, last_accessed
        FROM urls
        WHERE short_code = $shortCode
      """.query[URL].option.transact(xa)
    
    override def findByCustomAlias(alias: String): Task[Option[URL]] =
      sql"""
        SELECT id, short_code, original_url, custom_alias, title, description,
               user_id, password_hash, is_active, expires_at, click_count,
               created_at, updated_at, last_accessed
        FROM urls
        WHERE custom_alias = $alias
      """.query[URL].option.transact(xa)
    
    override def findByUserId(userId: Long, offset: Int, limit: Int): Task[List[URL]] =
      sql"""
        SELECT id, short_code, original_url, custom_alias, title, description,
               user_id, password_hash, is_active, expires_at, click_count,
               created_at, updated_at, last_accessed
        FROM urls
        WHERE user_id = $userId
        ORDER BY created_at DESC
        LIMIT $limit OFFSET $offset
      """.query[URL].to[List].transact(xa)
    
    override def searchByUserId(userId: Long, query: String, offset: Int, limit: Int): Task[List[URL]] =
      val searchPattern = s"%$query%"
      sql"""
        SELECT id, short_code, original_url, custom_alias, title, description,
               user_id, password_hash, is_active, expires_at, click_count,
               created_at, updated_at, last_accessed
        FROM urls
        WHERE user_id = $userId
          AND (title ILIKE $searchPattern
               OR description ILIKE $searchPattern
               OR original_url ILIKE $searchPattern
               OR short_code ILIKE $searchPattern
               OR custom_alias ILIKE $searchPattern)
        ORDER BY created_at DESC
        LIMIT $limit OFFSET $offset
      """.query[URL].to[List].transact(xa)
    
    override def countByUserId(userId: Long): Task[Long] =
      sql"SELECT COUNT(*) FROM urls WHERE user_id = $userId"
        .query[Long].unique.transact(xa)
    
    override def create(url: URL): Task[URL] =
      sql"""
        INSERT INTO urls (short_code, original_url, custom_alias, title, description,
                         user_id, password_hash, is_active, expires_at, click_count,
                         created_at, updated_at, last_accessed)
        VALUES (${url.shortCode}, ${url.originalUrl}, ${url.customAlias}, ${url.title},
                ${url.description}, ${url.userId}, ${url.passwordHash}, ${url.isActive},
                ${url.expiresAt}, ${url.clickCount}, ${url.createdAt}, ${url.updatedAt},
                ${url.lastAccessed})
        RETURNING id, short_code, original_url, custom_alias, title, description,
                  user_id, password_hash, is_active, expires_at, click_count,
                  created_at, updated_at, last_accessed
      """.query[URL].unique.transact(xa)
    
    override def update(url: URL): Task[URL] =
      sql"""
        UPDATE urls
        SET original_url = ${url.originalUrl},
            title = ${url.title},
            description = ${url.description},
            is_active = ${url.isActive},
            expires_at = ${url.expiresAt},
            updated_at = ${Instant.now()}
        WHERE id = ${url.id}
        RETURNING id, short_code, original_url, custom_alias, title, description,
                  user_id, password_hash, is_active, expires_at, click_count,
                  created_at, updated_at, last_accessed
      """.query[URL].unique.transact(xa)
    
    override def delete(id: Long): Task[Unit] =
      sql"DELETE FROM urls WHERE id = $id".update.run.transact(xa).unit
    
    override def incrementClickCount(id: Long): Task[Unit] =
      sql"""
        UPDATE urls
        SET click_count = click_count + 1
        WHERE id = $id
      """.update.run.transact(xa).unit
    
    override def updateLastAccessed(id: Long): Task[Unit] =
      sql"""
        UPDATE urls
        SET last_accessed = ${Instant.now()}
        WHERE id = $id
      """.update.run.transact(xa).unit
  
  val layer: ZLayer[Transactor[Task], Nothing, URLRepository] =
    ZLayer.fromFunction(Live.apply)

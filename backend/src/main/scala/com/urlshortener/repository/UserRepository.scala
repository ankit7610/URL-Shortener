package com.urlshortener.repository

import cats.effect.*
import doobie.*
import doobie.implicits.*
import doobie.postgres.implicits.*
import zio.*
import zio.interop.catz.*
import com.urlshortener.domain.User
import java.time.Instant

trait UserRepository:
  def findById(id: Long): Task[Option[User]]
  def findByEmail(email: String): Task[Option[User]]
  def findByApiKey(apiKey: String): Task[Option[User]]
  def create(user: User): Task[User]
  def update(user: User): Task[User]
  def delete(id: Long): Task[Unit]
  def updateLastLogin(id: Long): Task[Unit]

object UserRepository:
  
  case class Live(xa: Transactor[Task]) extends UserRepository:
    
    override def findById(id: Long): Task[Option[User]] =
      sql"""
        SELECT id, email, hashed_password, full_name, is_active, is_verified, is_admin,
               api_key, created_at, updated_at, last_login
        FROM users
        WHERE id = $id
      """.query[User].option.transact(xa)
    
    override def findByEmail(email: String): Task[Option[User]] =
      sql"""
        SELECT id, email, hashed_password, full_name, is_active, is_verified, is_admin,
               api_key, created_at, updated_at, last_login
        FROM users
        WHERE email = $email
      """.query[User].option.transact(xa)
    
    override def findByApiKey(apiKey: String): Task[Option[User]] =
      sql"""
        SELECT id, email, hashed_password, full_name, is_active, is_verified, is_admin,
               api_key, created_at, updated_at, last_login
        FROM users
        WHERE api_key = $apiKey
      """.query[User].option.transact(xa)
    
    override def create(user: User): Task[User] =
      sql"""
        INSERT INTO users (email, hashed_password, full_name, is_active, is_verified, is_admin,
                          api_key, created_at, updated_at, last_login)
        VALUES (${user.email}, ${user.hashedPassword}, ${user.fullName}, ${user.isActive},
                ${user.isVerified}, ${user.isAdmin}, ${user.apiKey}, ${user.createdAt},
                ${user.updatedAt}, ${user.lastLogin})
        RETURNING id, email, hashed_password, full_name, is_active, is_verified, is_admin,
                  api_key, created_at, updated_at, last_login
      """.query[User].unique.transact(xa)
    
    override def update(user: User): Task[User] =
      sql"""
        UPDATE users
        SET email = ${user.email},
            hashed_password = ${user.hashedPassword},
            full_name = ${user.fullName},
            is_active = ${user.isActive},
            is_verified = ${user.isVerified},
            is_admin = ${user.isAdmin},
            api_key = ${user.apiKey},
            updated_at = ${Instant.now()}
        WHERE id = ${user.id}
        RETURNING id, email, hashed_password, full_name, is_active, is_verified, is_admin,
                  api_key, created_at, updated_at, last_login
      """.query[User].unique.transact(xa)
    
    override def delete(id: Long): Task[Unit] =
      sql"DELETE FROM users WHERE id = $id".update.run.transact(xa).unit
    
    override def updateLastLogin(id: Long): Task[Unit] =
      sql"""
        UPDATE users
        SET last_login = ${Instant.now()}
        WHERE id = $id
      """.update.run.transact(xa).unit
  
  val layer: ZLayer[Transactor[Task], Nothing, UserRepository] =
    ZLayer.fromFunction(Live.apply)

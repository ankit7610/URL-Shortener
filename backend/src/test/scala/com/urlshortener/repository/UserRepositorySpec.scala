package com.urlshortener.repository

import zio.*
import zio.test.*
import zio.test.Assertion.*
import doobie.*
import com.urlshortener.domain.User
import java.time.Instant

object UserRepositorySpec extends ZIOSpecDefault:
  
  def spec = suite("UserRepositorySpec")(
    test("create should insert a new user") {
      for
        repo <- ZIO.service[UserRepository]
        xa <- ZIO.service[Transactor[Task]]
        
        user = User.create("test@example.com", "hashed_password", Some("Test User"))
        created <- repo.create(user)
        
        // Verify it was inserted
        found <- repo.findById(created.id)
      yield 
        assertTrue(created.id > 0) &&
        assertTrue(created.email == "test@example.com") &&
        assertTrue(found.isDefined) &&
        assertTrue(found.get.email == "test@example.com")
    },
    
    test("findByEmail should return user when exists") {
      for
        repo <- ZIO.service[UserRepository]
        xa <- ZIO.service[Transactor[Task]]
        
        user = User.create("find@example.com", "hash", Some("Find Me"))
        _ <- repo.create(user)
        
        found <- repo.findByEmail("find@example.com")
      yield 
        assertTrue(found.isDefined) &&
        assertTrue(found.get.email == "find@example.com") &&
        assertTrue(found.get.fullName == Some("Find Me"))
    },
    
    test("findByEmail should return None when not exists") {
      for
        repo <- ZIO.service[UserRepository]
        found <- repo.findByEmail("nonexistent@example.com")
      yield 
        assertTrue(found.isEmpty)
    },
    
    test("create should fail on duplicate email") {
      for
        repo <- ZIO.service[UserRepository]
        
        user1 = User.create("duplicate@example.com", "hash1", None)
        user2 = User.create("duplicate@example.com", "hash2", None)
        
        _ <- repo.create(user1)
        result <- repo.create(user2).either
      yield 
        assertTrue(result.isLeft) // Should fail due to unique constraint
    },
    
    test("update should modify user fields") {
      for
        repo <- ZIO.service[UserRepository]
        
        user = User.create("update@example.com", "hash", Some("Original"))
        created <- repo.create(user)
        
        updated = created.copy(fullName = Some("Updated Name"), isVerified = true)
        _ <- repo.update(updated)
        
        found <- repo.findById(created.id)
      yield 
        assertTrue(found.isDefined) &&
        assertTrue(found.get.fullName == Some("Updated Name")) &&
        assertTrue(found.get.isVerified)
    },
    
    test("delete should remove user") {
      for
        repo <- ZIO.service[UserRepository]
        
        user = User.create("delete@example.com", "hash", None)
        created <- repo.create(user)
        
        _ <- repo.delete(created.id)
        found <- repo.findById(created.id)
      yield 
        assertTrue(found.isEmpty)
    },
    
    test("updateLastLogin should update timestamp") {
      for
        repo <- ZIO.service[UserRepository]
        
        user = User.create("login@example.com", "hash", None)
        created <- repo.create(user)
        
        _ <- ZIO.sleep(100.millis) // Ensure time difference
        _ <- repo.updateLastLogin(created.id)
        
        found <- repo.findById(created.id)
      yield 
        assertTrue(found.isDefined) &&
        assertTrue(found.get.lastLogin.isDefined)
    },
    
    test("findByApiKey should return user when key exists") {
      for
        repo <- ZIO.service[UserRepository]
        
        user = User.create("api@example.com", "hash", None).copy(apiKey = Some("test-api-key-123"))
        created <- repo.create(user)
        
        found <- repo.findByApiKey("test-api-key-123")
      yield 
        assertTrue(found.isDefined) &&
        assertTrue(found.get.email == "api@example.com")
    }
  ).provide(
    DatabaseTestUtils.createTestDatabase,
    UserRepository.layer
  ) @@ TestAspect.beforeEach(
    ZIO.serviceWithZIO[Transactor[Task]](DatabaseTestUtils.cleanDatabase)
  ) @@ TestAspect.sequential

package com.urlshortener.repository

import zio.*
import zio.test.*
import zio.test.Assertion.*
import doobie.*
import com.urlshortener.domain.{URL => DomainURL}
import java.time.Instant
import java.time.temporal.ChronoUnit

object URLRepositorySpec extends ZIOSpecDefault:
  
  def spec = suite("URLRepositorySpec")(
    test("create should insert a new URL") {
      for
        repo <- ZIO.service[URLRepository]
        
        url = DomainURL.create("abc123", "https://example.com", title = Some("Test"))
        created <- repo.create(url)
        
        found <- repo.findById(created.id)
      yield 
        assertTrue(created.id > 0) &&
        assertTrue(created.shortCode == "abc123") &&
        assertTrue(found.isDefined) &&
        assertTrue(found.get.originalUrl == "https://example.com")
    },
    
    test("findByShortCode should return URL when exists") {
      for
        repo <- ZIO.service[URLRepository]
        
        url = DomainURL.create("short1", "https://google.com")
        _ <- repo.create(url)
        
        found <- repo.findByShortCode("short1")
      yield 
        assertTrue(found.isDefined) &&
        assertTrue(found.get.originalUrl == "https://google.com")
    },
    
    test("findByCustomAlias should return URL when exists") {
      for
        repo <- ZIO.service[URLRepository]
        
        url = DomainURL.create("xyz", "https://github.com", customAlias = Some("my-github"))
        _ <- repo.create(url)
        
        found <- repo.findByCustomAlias("my-github")
      yield 
        assertTrue(found.isDefined) &&
        assertTrue(found.get.shortCode == "xyz")
    },
    
    test("create should fail on duplicate short_code") {
      for
        repo <- ZIO.service[URLRepository]
        
        url1 = DomainURL.create("dup123", "https://url1.com")
        url2 = DomainURL.create("dup123", "https://url2.com")
        
        _ <- repo.create(url1)
        result <- repo.create(url2).either
      yield 
        assertTrue(result.isLeft)
    },
    
    test("incrementClickCount should increase counter") {
      for
        repo <- ZIO.service[URLRepository]
        
        url = DomainURL.create("click1", "https://example.com")
        created <- repo.create(url)
        
        _ <- repo.incrementClickCount(created.id)
        _ <- repo.incrementClickCount(created.id)
        
        found <- repo.findById(created.id)
      yield 
        assertTrue(found.isDefined) &&
        assertTrue(found.get.clickCount == 2)
    },
    
    test("updateLastAccessed should update timestamp") {
      for
        repo <- ZIO.service[URLRepository]
        
        url = DomainURL.create("access1", "https://example.com")
        created <- repo.create(url)
        
        _ <- ZIO.sleep(100.millis)
        _ <- repo.updateLastAccessed(created.id)
        
        found <- repo.findById(created.id)
      yield 
        assertTrue(found.isDefined) &&
        assertTrue(found.get.lastAccessed.isDefined)
    },
    
    test("update should modify URL fields") {
      for
        repo <- ZIO.service[URLRepository]
        
        url = DomainURL.create("upd1", "https://old.com")
        created <- repo.create(url)
        
        updated = created.copy(
          originalUrl = "https://new.com",
          title = Some("Updated Title"),
          isActive = false
        )
        _ <- repo.update(updated)
        
        found <- repo.findById(created.id)
      yield 
        assertTrue(found.isDefined) &&
        assertTrue(found.get.originalUrl == "https://new.com") &&
        assertTrue(found.get.title == Some("Updated Title")) &&
        assertTrue(!found.get.isActive)
    },
    
    test("delete should remove URL") {
      for
        repo <- ZIO.service[URLRepository]
        
        url = DomainURL.create("del1", "https://example.com")
        created <- repo.create(url)
        
        _ <- repo.delete(created.id)
        found <- repo.findById(created.id)
      yield 
        assertTrue(found.isEmpty)
    },
    
    test("findByUserId should return user's URLs with pagination") {
      for
        repo <- ZIO.service[URLRepository]
        
        url1 = DomainURL.create("u1", "https://url1.com", userId = Some(1L))
        url2 = DomainURL.create("u2", "https://url2.com", userId = Some(1L))
        url3 = DomainURL.create("u3", "https://url3.com", userId = Some(2L))
        
        _ <- repo.create(url1)
        _ <- repo.create(url2)
        _ <- repo.create(url3)
        
        userUrls <- repo.findByUserId(1L, offset = 0, limit = 10)
        count <- repo.countByUserId(1L)
      yield 
        assertTrue(userUrls.length == 2) &&
        assertTrue(count == 2)
    },
    
    test("searchByUserId should filter by query") {
      for
        repo <- ZIO.service[URLRepository]
        
        url1 = DomainURL.create("s1", "https://github.com", userId = Some(1L), title = Some("GitHub"))
        url2 = DomainURL.create("s2", "https://gitlab.com", userId = Some(1L), title = Some("GitLab"))
        
        _ <- repo.create(url1)
        _ <- repo.create(url2)
        
        results <- repo.searchByUserId(1L, "github", offset = 0, limit = 10)
      yield 
        assertTrue(results.length == 1) &&
        assertTrue(results.head.title == Some("GitHub"))
    },
    
    test("findByUserId should handle pagination edge cases - offset beyond data") {
      for
        repo <- ZIO.service[URLRepository]
        
        url1 = DomainURL.create("p1", "https://url1.com", userId = Some(1L))
        url2 = DomainURL.create("p2", "https://url2.com", userId = Some(1L))
        
        _ <- repo.create(url1)
        _ <- repo.create(url2)
        
        results <- repo.findByUserId(1L, offset = 100, limit = 10)
      yield 
        assertTrue(results.isEmpty)
    },
    
    test("findByUserId should handle zero limit") {
      for
        repo <- ZIO.service[URLRepository]
        
        url = DomainURL.create("z1", "https://example.com", userId = Some(1L))
        _ <- repo.create(url)
        
        results <- repo.findByUserId(1L, offset = 0, limit = 0)
      yield 
        assertTrue(results.isEmpty)
    },
    
    test("searchByUserId should handle special characters in query") {
      for
        repo <- ZIO.service[URLRepository]
        
        url1 = DomainURL.create("sp1", "https://test.com", userId = Some(1L), title = Some("Test%Title"))
        _ <- repo.create(url1)
        
        results <- repo.searchByUserId(1L, "%", offset = 0, limit = 10)
      yield 
        assertTrue(results.nonEmpty)
    },
    
    test("findByShortCode should return None for non-existent code") {
      for
        repo <- ZIO.service[URLRepository]
        found <- repo.findByShortCode("nonexistent123")
      yield 
        assertTrue(found.isEmpty)
    },
    
    test("create should handle URLs with all optional fields as None") {
      for
        repo <- ZIO.service[URLRepository]
        
        url = DomainURL.create("min1", "https://minimal.com")
        created <- repo.create(url)
        
        found <- repo.findById(created.id)
      yield 
        assertTrue(found.isDefined) &&
        assertTrue(found.get.title.isEmpty) &&
        assertTrue(found.get.customAlias.isEmpty) &&
        assertTrue(found.get.userId.isEmpty)
    },
    
    test("countByUserId should return 0 for user with no URLs") {
      for
        repo <- ZIO.service[URLRepository]
        count <- repo.countByUserId(999L)
      yield 
        assertTrue(count == 0)
    },
    
    test("searchByUserId should return empty for non-matching query") {
      for
        repo <- ZIO.service[URLRepository]
        
        url = DomainURL.create("nm1", "https://example.com", userId = Some(1L), title = Some("Example"))
        _ <- repo.create(url)
        
        results <- repo.searchByUserId(1L, "nonmatching", offset = 0, limit = 10)
      yield 
        assertTrue(results.isEmpty)
    }
  ).provide(
    DatabaseTestUtils.createTestDatabase,
    URLRepository.layer
  ) @@ TestAspect.beforeEach(
    ZIO.serviceWithZIO[Transactor[Task]](DatabaseTestUtils.cleanDatabase)
  ) @@ TestAspect.sequential

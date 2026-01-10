package com.urlshortener.repository

import zio.*
import doobie.*
import doobie.implicits.*
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import org.flywaydb.core.Flyway
import java.io.File

object DatabaseTestUtils:
  
  /**
   * Creates an embedded PostgreSQL instance for testing.
   * Automatically runs Flyway migrations.
   */
  def createTestDatabase: ZLayer[Any, Throwable, Transactor[Task]] =
    ZLayer.scoped {
      for
        // Start embedded PostgreSQL
        postgres <- ZIO.fromAutoCloseable(
          ZIO.attempt(EmbeddedPostgres.builder().start())
        )
        
        // Get JDBC URL
        jdbcUrl = postgres.getJdbcUrl("postgres", "postgres")
        
        // Run Flyway migrations
        _ <- ZIO.attempt {
          val flyway = Flyway.configure()
            .dataSource(jdbcUrl, "postgres", "postgres")
            .locations("classpath:db/migration")
            .load()
          flyway.migrate()
        }
        
        // Create Transactor
        xa <- ZIO.attempt {
          Transactor.fromDriverManager[Task](
            driver = "org.postgresql.Driver",
            url = jdbcUrl,
            user = "postgres",
            password = "postgres",
            logHandler = None
          )
        }
      yield xa
    }
  
  /**
   * Cleans all tables in the database (for test isolation).
   */
  def cleanDatabase(xa: Transactor[Task]): Task[Unit] =
    sql"""
      TRUNCATE TABLE analytics, urls, users RESTART IDENTITY CASCADE
    """.update.run.transact(xa).unit

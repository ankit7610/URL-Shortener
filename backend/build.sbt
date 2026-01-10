name := "url-shortener"
version := "1.0.0"
scalaVersion := "3.6.2"

// Compiler options
scalacOptions ++= Seq(
  "-encoding", "UTF-8",
  "-feature",
  "-unchecked",
  "-deprecation",
  "-Xfatal-warnings"
)

// Dependency versions
val zioVersion = "2.1.14"
val http4sVersion = "0.23.30"
val doobieVersion = "1.0.0-RC7"
val circeVersion = "0.14.10"
val redis4catsVersion = "1.7.1"
val logbackVersion = "1.5.12"
val flywayVersion = "11.1.0"

libraryDependencies ++= Seq(
  // ZIO Core
  "dev.zio" %% "zio" % zioVersion,
  "dev.zio" %% "zio-streams" % zioVersion,
  "dev.zio" %% "zio-config" % "4.0.2",
  "dev.zio" %% "zio-config-typesafe" % "4.0.2",
  "dev.zio" %% "zio-logging" % "2.4.0",
  "dev.zio" %% "zio-logging-slf4j2" % "2.4.0",
  "dev.zio" %% "zio-metrics-connectors" % "2.3.1",
  "dev.zio" %% "zio-metrics-connectors-prometheus" % "2.3.1",
  
  // HTTP4s
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-ember-server" % http4sVersion,
  "org.http4s" %% "http4s-ember-client" % http4sVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
  
  // Doobie (Database)
  "org.tpolecat" %% "doobie-core" % doobieVersion,
  "org.tpolecat" %% "doobie-postgres" % doobieVersion,
  "org.tpolecat" %% "doobie-hikari" % doobieVersion,
  
  // Circe (JSON)
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  
  // Redis
  "dev.profunktor" %% "redis4cats-effects" % redis4catsVersion,
  "dev.profunktor" %% "redis4cats-streams" % redis4catsVersion,
  
  // Security
  "com.github.t3hnar" %% "scala-bcrypt" % "4.3.0",
  "com.github.jwt-scala" %% "jwt-circe" % "10.0.1",
  
  // QR Code
  "com.github.kenglxn.qrgen" % "javase" % "3.0.1",
  
  // Database Migrations
  "org.flywaydb" % "flyway-core" % flywayVersion,
  "org.flywaydb" % "flyway-database-postgresql" % flywayVersion,
  
  // Logging
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  
  // User Agent Parsing
  "eu.bitwalker" % "UserAgentUtils" % "1.21",
  
  // Testing
  "dev.zio" %% "zio-test" % zioVersion % Test,
  "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
  "org.tpolecat" %% "doobie-scalatest" % doobieVersion % Test,
  
  // Database Testing
  "io.zonky.test" % "embedded-postgres" % "2.0.7" % Test,
  "org.testcontainers" % "postgresql" % "1.19.3" % Test
)

// Test framework
testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

// Assembly settings for fat JAR
assembly / assemblyMergeStrategy := {
  case PathList("META-INF", "maven", "org.webjars", "swagger-ui", "pom.properties") => MergeStrategy.singleOrError
  case PathList("META-INF", xs @ _*) => 
    xs match {
      case "MANIFEST.MF" :: Nil => MergeStrategy.discard
      case "services" :: _ => MergeStrategy.concat
      case _ => MergeStrategy.discard
    }
  case "application.conf" => MergeStrategy.concat
  case "reference.conf" => MergeStrategy.concat
  case _ => MergeStrategy.first
}

assembly / assemblyJarName := "url-shortener.jar"
assembly / mainClass := Some("com.urlshortener.Main")

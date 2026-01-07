"""Application configuration using Pydantic Settings"""

from typing import List
from pydantic import field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Application settings with environment variable support"""
    
    # Application
    APP_NAME: str = "URL Shortener"
    APP_VERSION: str = "1.0.0"
    ENVIRONMENT: str = "development"
    DEBUG: bool = True
    
    # Database
    DATABASE_URL: str
    DATABASE_POOL_SIZE: int = 20
    DATABASE_MAX_OVERFLOW: int = 10
    
    # Redis
    REDIS_URL: str
    REDIS_MAX_CONNECTIONS: int = 10
    
    # JWT
    JWT_SECRET_KEY: str
    JWT_ALGORITHM: str = "HS256"
    JWT_ACCESS_TOKEN_EXPIRE_MINUTES: int = 30
    JWT_REFRESH_TOKEN_EXPIRE_DAYS: int = 7
    
    # CORS
    ALLOWED_ORIGINS: str = "http://localhost:3000"
    
    @field_validator("ALLOWED_ORIGINS")
    @classmethod
    def parse_cors_origins(cls, v: str) -> List[str]:
        """Parse comma-separated CORS origins"""
        return [origin.strip() for origin in v.split(",")]
    
    # Security
    RATE_LIMIT_PER_MINUTE: int = 60
    RATE_LIMIT_PER_HOUR: int = 1000
    PASSWORD_MIN_LENGTH: int = 8
    
    # URL Shortening
    SHORT_URL_LENGTH: int = 7
    CUSTOM_ALIAS_MIN_LENGTH: int = 3
    CUSTOM_ALIAS_MAX_LENGTH: int = 50
    
    # Cache TTL (seconds)
    CACHE_TTL_HOT_URL: int = 3600  # 1 hour
    CACHE_TTL_ANALYTICS: int = 300  # 5 minutes
    
    # External Services
    SENTRY_DSN: str = ""
    GEOIP_DATABASE_PATH: str = "./data/GeoLite2-City.mmdb"
    
    # Email (Optional)
    SMTP_HOST: str = ""
    SMTP_PORT: int = 587
    SMTP_USER: str = ""
    SMTP_PASSWORD: str = ""
    EMAIL_FROM: str = "noreply@urlshortener.com"
    
    model_config = SettingsConfigDict(
        env_file=".env",
        case_sensitive=True,
        extra="ignore"
    )
    
    @property
    def is_production(self) -> bool:
        """Check if running in production environment"""
        return self.ENVIRONMENT.lower() == "production"
    
    @property
    def is_development(self) -> bool:
        """Check if running in development environment"""
        return self.ENVIRONMENT.lower() == "development"


# Global settings instance
settings = Settings()

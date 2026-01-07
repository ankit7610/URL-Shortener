"""Redis cache connection and utilities"""

import json
from typing import Any, Optional
import redis.asyncio as redis
from redis.asyncio import Redis
from redis.exceptions import RedisError

from app.core.config import settings
from app.utils.logger import logger


class RedisCache:
    """Redis cache manager with graceful degradation"""
    
    def __init__(self):
        self._redis: Optional[Redis] = None
        self._available = False
    
    async def connect(self) -> None:
        """Establish Redis connection"""
        try:
            self._redis = await redis.from_url(
                settings.REDIS_URL,
                encoding="utf-8",
                decode_responses=True,
                max_connections=settings.REDIS_MAX_CONNECTIONS,
            )
            # Test connection
            await self._redis.ping()
            self._available = True
            logger.info("Redis connection established")
        except RedisError as e:
            logger.error(f"Redis connection failed: {e}")
            self._available = False
    
    async def close(self) -> None:
        """Close Redis connection"""
        if self._redis:
            await self._redis.close()
            logger.info("Redis connection closed")
    
    async def get(self, key: str) -> Optional[Any]:
        """
        Get value from cache.
        Returns None if key doesn't exist or cache is unavailable.
        """
        if not self._available or not self._redis:
            return None
        
        try:
            value = await self._redis.get(key)
            if value:
                return json.loads(value)
            return None
        except RedisError as e:
            logger.warning(f"Redis GET error for key {key}: {e}")
            return None
    
    async def set(
        self,
        key: str,
        value: Any,
        ttl: Optional[int] = None
    ) -> bool:
        """
        Set value in cache with optional TTL.
        Returns True if successful, False otherwise.
        """
        if not self._available or not self._redis:
            return False
        
        try:
            serialized = json.dumps(value)
            if ttl:
                await self._redis.setex(key, ttl, serialized)
            else:
                await self._redis.set(key, serialized)
            return True
        except (RedisError, TypeError) as e:
            logger.warning(f"Redis SET error for key {key}: {e}")
            return False
    
    async def delete(self, key: str) -> bool:
        """Delete key from cache"""
        if not self._available or not self._redis:
            return False
        
        try:
            await self._redis.delete(key)
            return True
        except RedisError as e:
            logger.warning(f"Redis DELETE error for key {key}: {e}")
            return False
    
    async def increment(self, key: str, amount: int = 1) -> Optional[int]:
        """Increment counter. Returns new value or None on error."""
        if not self._available or not self._redis:
            return None
        
        try:
            return await self._redis.incrby(key, amount)
        except RedisError as e:
            logger.warning(f"Redis INCREMENT error for key {key}: {e}")
            return None
    
    async def expire(self, key: str, ttl: int) -> bool:
        """Set TTL on existing key"""
        if not self._available or not self._redis:
            return False
        
        try:
            await self._redis.expire(key, ttl)
            return True
        except RedisError as e:
            logger.warning(f"Redis EXPIRE error for key {key}: {e}")
            return False
    
    @property
    def is_available(self) -> bool:
        """Check if Redis is available"""
        return self._available


# Global cache instance
cache = RedisCache()


async def get_cache() -> RedisCache:
    """Dependency for getting cache instance"""
    return cache

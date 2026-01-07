"""URL shortening service with Base62 encoding"""

import string
import secrets
from typing import Optional
from urllib.parse import urlparse
import httpx

from app.core.config import settings
from app.utils.logger import logger


class URLShortener:
    """URL shortening service with Base62 encoding and collision handling"""
    
    # Base62 alphabet (0-9, a-z, A-Z)
    ALPHABET = string.digits + string.ascii_lowercase + string.ascii_uppercase
    BASE = len(ALPHABET)
    
    # Malicious URL patterns (basic detection)
    MALICIOUS_PATTERNS = [
        "javascript:",
        "data:",
        "vbscript:",
        "file:",
    ]
    
    @classmethod
    def encode(cls, num: int) -> str:
        """
        Encode integer to Base62 string.
        
        Args:
            num: Integer to encode
        
        Returns:
            Base62 encoded string
        
        Example:
            encode(1000) -> "G8"
        """
        if num == 0:
            return cls.ALPHABET[0]
        
        result = []
        while num:
            num, remainder = divmod(num, cls.BASE)
            result.append(cls.ALPHABET[remainder])
        
        return ''.join(reversed(result))
    
    @classmethod
    def decode(cls, short_code: str) -> int:
        """
        Decode Base62 string to integer.
        
        Args:
            short_code: Base62 string to decode
        
        Returns:
            Decoded integer
        
        Example:
            decode("G8") -> 1000
        """
        num = 0
        for char in short_code:
            num = num * cls.BASE + cls.ALPHABET.index(char)
        return num
    
    @classmethod
    def generate_short_code(cls, url_id: int, retry_count: int = 0) -> str:
        """
        Generate short code from URL ID with collision handling.
        
        Args:
            url_id: Database ID of the URL
            retry_count: Number of retries for collision handling
        
        Returns:
            Short code string
        """
        # Add retry count to handle collisions
        adjusted_id = url_id + (retry_count * 1000000)
        short_code = cls.encode(adjusted_id)
        
        # Ensure minimum length by padding with random characters
        if len(short_code) < settings.SHORT_URL_LENGTH:
            padding_length = settings.SHORT_URL_LENGTH - len(short_code)
            padding = ''.join(secrets.choice(cls.ALPHABET) for _ in range(padding_length))
            short_code = padding + short_code
        
        return short_code
    
    @classmethod
    def generate_random_code(cls, length: Optional[int] = None) -> str:
        """
        Generate random short code (for custom aliases or fallback).
        
        Args:
            length: Length of the code (default: SHORT_URL_LENGTH)
        
        Returns:
            Random short code
        """
        length = length or settings.SHORT_URL_LENGTH
        return ''.join(secrets.choice(cls.ALPHABET) for _ in range(length))
    
    @classmethod
    def validate_custom_alias(cls, alias: str) -> tuple[bool, Optional[str]]:
        """
        Validate custom alias.
        
        Args:
            alias: Custom alias to validate
        
        Returns:
            Tuple of (is_valid, error_message)
        """
        # Check length
        if len(alias) < settings.CUSTOM_ALIAS_MIN_LENGTH:
            return False, f"Alias must be at least {settings.CUSTOM_ALIAS_MIN_LENGTH} characters"
        
        if len(alias) > settings.CUSTOM_ALIAS_MAX_LENGTH:
            return False, f"Alias must be at most {settings.CUSTOM_ALIAS_MAX_LENGTH} characters"
        
        # Check characters (alphanumeric, dash, underscore only)
        allowed_chars = set(cls.ALPHABET + "-_")
        if not all(c in allowed_chars for c in alias):
            return False, "Alias can only contain letters, numbers, dashes, and underscores"
        
        # Check for reserved words
        reserved = {"api", "admin", "dashboard", "health", "metrics", "docs", "swagger"}
        if alias.lower() in reserved:
            return False, "This alias is reserved"
        
        return True, None
    
    @classmethod
    async def validate_url(cls, url: str) -> tuple[bool, Optional[str]]:
        """
        Validate URL for safety and accessibility.
        
        Args:
            url: URL to validate
        
        Returns:
            Tuple of (is_valid, error_message)
        """
        # Check for malicious patterns
        url_lower = url.lower()
        for pattern in cls.MALICIOUS_PATTERNS:
            if url_lower.startswith(pattern):
                return False, "URL contains potentially malicious protocol"
        
        # Parse URL
        try:
            parsed = urlparse(url)
            if not parsed.scheme or not parsed.netloc:
                return False, "Invalid URL format"
            
            # Only allow http and https
            if parsed.scheme not in ("http", "https"):
                return False, "Only HTTP and HTTPS URLs are allowed"
        except Exception as e:
            logger.error(f"URL parsing error: {e}")
            return False, "Invalid URL format"
        
        # Optional: Check if URL is accessible (can be slow)
        # Disabled by default to avoid performance issues
        # try:
        #     async with httpx.AsyncClient(timeout=5.0) as client:
        #         response = await client.head(url, follow_redirects=True)
        #         if response.status_code >= 400:
        #             return False, "URL is not accessible"
        # except Exception as e:
        #     logger.warning(f"URL accessibility check failed: {e}")
        #     # Don't fail validation if check fails
        
        return True, None


# Global instance
url_shortener = URLShortener()

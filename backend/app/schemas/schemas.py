"""Pydantic schemas for request/response validation"""

from datetime import datetime
from typing import Optional
from pydantic import BaseModel, EmailStr, Field, HttpUrl, field_validator


# ============================================================================
# User Schemas
# ============================================================================

class UserBase(BaseModel):
    """Base user schema"""
    email: EmailStr
    full_name: Optional[str] = None


class UserCreate(UserBase):
    """Schema for user registration"""
    password: str = Field(..., min_length=8)


class UserLogin(BaseModel):
    """Schema for user login"""
    email: EmailStr
    password: str


class UserResponse(UserBase):
    """Schema for user response"""
    id: int
    is_active: bool
    is_verified: bool
    is_admin: bool
    created_at: datetime
    
    class Config:
        from_attributes = True


class UserWithAPIKey(UserResponse):
    """Schema for user response with API key"""
    api_key: Optional[str] = None


class TokenResponse(BaseModel):
    """Schema for JWT token response"""
    access_token: str
    refresh_token: str
    token_type: str = "bearer"


# ============================================================================
# URL Schemas
# ============================================================================

class URLCreate(BaseModel):
    """Schema for creating a short URL"""
    original_url: HttpUrl
    custom_alias: Optional[str] = Field(None, min_length=3, max_length=50)
    title: Optional[str] = Field(None, max_length=255)
    description: Optional[str] = None
    password: Optional[str] = None
    expires_in_days: Optional[int] = Field(None, gt=0, le=365)
    
    @field_validator("custom_alias")
    @classmethod
    def validate_custom_alias(cls, v: Optional[str]) -> Optional[str]:
        """Validate custom alias format"""
        if v is None:
            return v
        
        # Only alphanumeric, dash, and underscore
        if not all(c.isalnum() or c in ("-", "_") for c in v):
            raise ValueError("Custom alias can only contain letters, numbers, dashes, and underscores")
        
        return v


class URLUpdate(BaseModel):
    """Schema for updating a URL"""
    original_url: Optional[HttpUrl] = None
    title: Optional[str] = Field(None, max_length=255)
    description: Optional[str] = None
    is_active: Optional[bool] = None
    expires_in_days: Optional[int] = Field(None, gt=0, le=365)


class URLResponse(BaseModel):
    """Schema for URL response"""
    id: int
    short_code: str
    original_url: str
    custom_alias: Optional[str]
    title: Optional[str]
    description: Optional[str]
    is_active: bool
    expires_at: Optional[datetime]
    click_count: int
    created_at: datetime
    updated_at: datetime
    last_accessed: Optional[datetime]
    
    # Computed fields
    short_url: Optional[str] = None
    qr_code_url: Optional[str] = None
    is_password_protected: bool = False
    
    class Config:
        from_attributes = True


class URLListResponse(BaseModel):
    """Schema for paginated URL list"""
    urls: list[URLResponse]
    total: int
    page: int
    page_size: int
    total_pages: int


# ============================================================================
# Analytics Schemas
# ============================================================================

class AnalyticsResponse(BaseModel):
    """Schema for analytics response"""
    id: int
    url_id: int
    clicked_at: datetime
    country: Optional[str]
    city: Optional[str]
    device_type: Optional[str]
    browser: Optional[str]
    os: Optional[str]
    referrer: Optional[str]
    
    class Config:
        from_attributes = True


class ClicksByDay(BaseModel):
    """Schema for clicks by day"""
    date: str
    count: int


class ClicksByCategory(BaseModel):
    """Schema for clicks by category (country, device, browser, etc.)"""
    category: str
    count: int


class URLAnalyticsSummary(BaseModel):
    """Schema for aggregated URL analytics"""
    total_clicks: int
    clicks_by_day: list[ClicksByDay]
    clicks_by_country: list[dict]
    clicks_by_device: list[dict]
    clicks_by_browser: list[dict]
    top_referrers: list[dict]


# ============================================================================
# QR Code Schemas
# ============================================================================

class QRCodeRequest(BaseModel):
    """Schema for QR code generation request"""
    size: int = Field(300, ge=100, le=1000)
    border: int = Field(2, ge=0, le=10)


# ============================================================================
# Generic Schemas
# ============================================================================

class MessageResponse(BaseModel):
    """Generic message response"""
    message: str
    detail: Optional[str] = None


class HealthResponse(BaseModel):
    """Health check response"""
    status: str
    version: str
    environment: str
    database: str
    cache: str

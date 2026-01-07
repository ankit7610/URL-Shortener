"""URL management routes"""

from datetime import datetime, timedelta
from typing import Optional
from fastapi import APIRouter, Depends, HTTPException, status, Request, Response
from fastapi.responses import RedirectResponse, StreamingResponse
from sqlalchemy import select, func, and_, or_
from sqlalchemy.ext.asyncio import AsyncSession
import io

from app.core.database import get_db
from app.core.cache import cache
from app.core.config import settings
from app.core.security import security
from app.models.url import URL
from app.models.user import User
from app.schemas.schemas import (
    URLCreate,
    URLUpdate,
    URLResponse,
    URLListResponse,
    URLAnalyticsSummary,
    QRCodeRequest,
    MessageResponse
)
from app.services.url_shortener import url_shortener
from app.services.analytics import analytics_service
from app.services.qr_code import qr_service
from app.api.dependencies import get_current_user, get_optional_user
from app.utils.logger import logger


router = APIRouter(tags=["URLs"])


def build_url_response(url: URL, base_url: str) -> URLResponse:
    """Build URL response with computed fields"""
    response = URLResponse.model_validate(url)
    response.short_url = f"{base_url}/{url.custom_alias or url.short_code}"
    response.qr_code_url = f"{base_url}/api/urls/{url.id}/qr"
    response.is_password_protected = url.password_hash is not None
    return response


@router.post("/api/urls", response_model=URLResponse, status_code=status.HTTP_201_CREATED)
async def create_short_url(
    url_data: URLCreate,
    request: Request,
    db: AsyncSession = Depends(get_db),
    current_user: Optional[User] = Depends(get_optional_user)
):
    """
    Create a new short URL.
    
    - **original_url**: The URL to shorten (required)
    - **custom_alias**: Custom short code (optional, must be unique)
    - **title**: Title for the link (optional)
    - **description**: Description (optional)
    - **password**: Password protection (optional)
    - **expires_in_days**: Expiration in days (optional)
    
    Authentication is optional. Authenticated users can manage their URLs.
    """
    # Validate URL
    is_valid, error_msg = await url_shortener.validate_url(str(url_data.original_url))
    if not is_valid:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=error_msg
        )
    
    # Check custom alias if provided
    if url_data.custom_alias:
        is_valid, error_msg = url_shortener.validate_custom_alias(url_data.custom_alias)
        if not is_valid:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=error_msg
            )
        
        # Check if alias already exists
        stmt = select(URL).where(URL.custom_alias == url_data.custom_alias)
        result = await db.execute(stmt)
        if result.scalar_one_or_none():
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail="Custom alias already exists"
            )
    
    # Calculate expiration
    expires_at = None
    if url_data.expires_in_days:
        expires_at = datetime.utcnow() + timedelta(days=url_data.expires_in_days)
    
    # Hash password if provided
    password_hash = None
    if url_data.password:
        password_hash = security.hash_password(url_data.password)
    
    # Create URL record
    new_url = URL(
        original_url=str(url_data.original_url),
        custom_alias=url_data.custom_alias,
        title=url_data.title,
        description=url_data.description,
        user_id=current_user.id if current_user else None,
        password_hash=password_hash,
        expires_at=expires_at,
        short_code="",  # Will be set after getting ID
    )
    
    db.add(new_url)
    await db.flush()  # Get ID without committing
    
    # Generate short code from ID
    retry_count = 0
    while True:
        short_code = url_shortener.generate_short_code(new_url.id, retry_count)
        
        # Check for collision
        stmt = select(URL).where(URL.short_code == short_code)
        result = await db.execute(stmt)
        if not result.scalar_one_or_none():
            new_url.short_code = short_code
            break
        
        retry_count += 1
        if retry_count > 10:
            raise HTTPException(
                status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
                detail="Failed to generate unique short code"
            )
    
    await db.commit()
    await db.refresh(new_url)
    
    logger.info(f"Short URL created: {new_url.short_code} -> {new_url.original_url}")
    
    # Build response
    base_url = str(request.base_url).rstrip("/")
    return build_url_response(new_url, base_url)


@router.get("/{short_code}")
async def redirect_short_url(
    short_code: str,
    request: Request,
    db: AsyncSession = Depends(get_db)
):
    """
    Redirect to original URL and track analytics.
    
    This is the main redirect endpoint.
    """
    # Try to get from cache first
    cache_key = f"url:{short_code}"
    cached_url = await cache.get(cache_key)
    
    if cached_url:
        original_url = cached_url.get("original_url")
        url_id = cached_url.get("id")
    else:
        # Get from database
        stmt = select(URL).where(
            or_(
                URL.short_code == short_code,
                URL.custom_alias == short_code
            )
        )
        result = await db.execute(stmt)
        url = result.scalar_one_or_none()
        
        if not url:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="Short URL not found"
            )
        
        # Check if URL is accessible
        if not url.is_accessible:
            if url.is_expired:
                raise HTTPException(
                    status_code=status.HTTP_410_GONE,
                    detail="This link has expired"
                )
            else:
                raise HTTPException(
                    status_code=status.HTTP_403_FORBIDDEN,
                    detail="This link is no longer active"
                )
        
        # TODO: Check password if protected
        
        original_url = url.original_url
        url_id = url.id
        
        # Cache for future requests
        await cache.set(
            cache_key,
            {"id": url_id, "original_url": original_url},
            ttl=settings.CACHE_TTL_HOT_URL
        )
    
    # Track analytics asynchronously (don't block redirect)
    # In production, this should be done via background task or message queue
    try:
        ip_address = request.client.host if request.client else None
        user_agent = request.headers.get("user-agent")
        referrer = request.headers.get("referer")
        
        await analytics_service.track_click(
            db=db,
            url_id=url_id,
            ip_address=ip_address,
            user_agent=user_agent,
            referrer=referrer
        )
    except Exception as e:
        logger.error(f"Analytics tracking error: {e}")
        # Don't fail redirect if analytics fails
    
    # Redirect to original URL
    return RedirectResponse(url=original_url, status_code=status.HTTP_307_TEMPORARY_REDIRECT)


@router.get("/api/urls", response_model=URLListResponse)
async def list_urls(
    page: int = 1,
    page_size: int = 20,
    search: Optional[str] = None,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """
    List URLs for the current user with pagination and search.
    
    - **page**: Page number (default: 1)
    - **page_size**: Items per page (default: 20, max: 100)
    - **search**: Search in title, description, or original URL
    """
    # Limit page size
    page_size = min(page_size, 100)
    offset = (page - 1) * page_size
    
    # Build query
    query = select(URL).where(URL.user_id == current_user.id)
    
    if search:
        search_pattern = f"%{search}%"
        query = query.where(
            or_(
                URL.title.ilike(search_pattern),
                URL.description.ilike(search_pattern),
                URL.original_url.ilike(search_pattern),
                URL.short_code.ilike(search_pattern),
                URL.custom_alias.ilike(search_pattern)
            )
        )
    
    # Get total count
    count_query = select(func.count()).select_from(URL).where(URL.user_id == current_user.id)
    if search:
        search_pattern = f"%{search}%"
        count_query = count_query.where(
            or_(
                URL.title.ilike(search_pattern),
                URL.description.ilike(search_pattern),
                URL.original_url.ilike(search_pattern)
            )
        )
    
    total_result = await db.execute(count_query)
    total = total_result.scalar()
    
    # Get paginated results
    query = query.order_by(URL.created_at.desc()).offset(offset).limit(page_size)
    result = await db.execute(query)
    urls = result.scalars().all()
    
    # Build responses
    base_url = "http://localhost:8000"  # TODO: Get from request
    url_responses = [build_url_response(url, base_url) for url in urls]
    
    total_pages = (total + page_size - 1) // page_size
    
    return URLListResponse(
        urls=url_responses,
        total=total,
        page=page,
        page_size=page_size,
        total_pages=total_pages
    )


@router.get("/api/urls/{url_id}", response_model=URLResponse)
async def get_url(
    url_id: int,
    request: Request,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get URL details by ID"""
    stmt = select(URL).where(and_(URL.id == url_id, URL.user_id == current_user.id))
    result = await db.execute(stmt)
    url = result.scalar_one_or_none()
    
    if not url:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="URL not found"
        )
    
    base_url = str(request.base_url).rstrip("/")
    return build_url_response(url, base_url)


@router.put("/api/urls/{url_id}", response_model=URLResponse)
async def update_url(
    url_id: int,
    url_data: URLUpdate,
    request: Request,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Update URL details"""
    stmt = select(URL).where(and_(URL.id == url_id, URL.user_id == current_user.id))
    result = await db.execute(stmt)
    url = result.scalar_one_or_none()
    
    if not url:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="URL not found"
        )
    
    # Update fields
    if url_data.original_url is not None:
        is_valid, error_msg = await url_shortener.validate_url(str(url_data.original_url))
        if not is_valid:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=error_msg
            )
        url.original_url = str(url_data.original_url)
    
    if url_data.title is not None:
        url.title = url_data.title
    
    if url_data.description is not None:
        url.description = url_data.description
    
    if url_data.is_active is not None:
        url.is_active = url_data.is_active
    
    if url_data.expires_in_days is not None:
        url.expires_at = datetime.utcnow() + timedelta(days=url_data.expires_in_days)
    
    await db.commit()
    await db.refresh(url)
    
    # Invalidate cache
    cache_key = f"url:{url.short_code}"
    await cache.delete(cache_key)
    if url.custom_alias:
        await cache.delete(f"url:{url.custom_alias}")
    
    logger.info(f"URL updated: {url.short_code}")
    
    base_url = str(request.base_url).rstrip("/")
    return build_url_response(url, base_url)


@router.delete("/api/urls/{url_id}", response_model=MessageResponse)
async def delete_url(
    url_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Delete URL"""
    stmt = select(URL).where(and_(URL.id == url_id, URL.user_id == current_user.id))
    result = await db.execute(stmt)
    url = result.scalar_one_or_none()
    
    if not url:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="URL not found"
        )
    
    # Invalidate cache
    cache_key = f"url:{url.short_code}"
    await cache.delete(cache_key)
    if url.custom_alias:
        await cache.delete(f"url:{url.custom_alias}")
    
    await db.delete(url)
    await db.commit()
    
    logger.info(f"URL deleted: {url.short_code}")
    
    return MessageResponse(message="URL deleted successfully")


@router.get("/api/urls/{url_id}/analytics", response_model=URLAnalyticsSummary)
async def get_url_analytics(
    url_id: int,
    days: int = 30,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """
    Get analytics for a URL.
    
    - **days**: Number of days to include (default: 30)
    """
    # Verify ownership
    stmt = select(URL).where(and_(URL.id == url_id, URL.user_id == current_user.id))
    result = await db.execute(stmt)
    url = result.scalar_one_or_none()
    
    if not url:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="URL not found"
        )
    
    # Get analytics
    analytics = await analytics_service.get_url_analytics(db, url_id, days)
    
    return URLAnalyticsSummary(**analytics)


@router.get("/api/urls/{url_id}/qr")
async def get_qr_code(
    url_id: int,
    size: int = 300,
    border: int = 2,
    request: Request,
    db: AsyncSession = Depends(get_db),
    current_user: Optional[User] = Depends(get_optional_user)
):
    """
    Generate QR code for a URL.
    
    - **size**: QR code size in pixels (100-1000, default: 300)
    - **border**: Border size in boxes (0-10, default: 2)
    
    Authentication is optional for public URLs.
    """
    # Get URL
    query = select(URL).where(URL.id == url_id)
    if current_user:
        query = query.where(URL.user_id == current_user.id)
    
    result = await db.execute(query)
    url = result.scalar_one_or_none()
    
    if not url:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="URL not found"
        )
    
    # Build short URL
    base_url = str(request.base_url).rstrip("/")
    short_url = f"{base_url}/{url.custom_alias or url.short_code}"
    
    # Generate QR code
    qr_bytes = qr_service.generate_qr_code(short_url, size, border)
    
    if not qr_bytes:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Failed to generate QR code"
        )
    
    # Return as image
    return StreamingResponse(
        io.BytesIO(qr_bytes),
        media_type="image/png",
        headers={"Content-Disposition": f"inline; filename=qr_{url.short_code}.png"}
    )

"""Authentication routes"""

from datetime import datetime, timedelta
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_db
from app.core.security import security
from app.models.user import User
from app.schemas.schemas import (
    UserCreate,
    UserLogin,
    UserResponse,
    UserWithAPIKey,
    TokenResponse,
    MessageResponse
)
from app.api.dependencies import get_current_user
from app.utils.logger import logger


router = APIRouter(prefix="/auth", tags=["Authentication"])


@router.post("/register", response_model=UserResponse, status_code=status.HTTP_201_CREATED)
async def register(
    user_data: UserCreate,
    db: AsyncSession = Depends(get_db)
):
    """
    Register a new user.
    
    - **email**: Valid email address
    - **password**: Minimum 8 characters with uppercase, lowercase, and digit
    - **full_name**: Optional full name
    """
    # Validate password strength
    is_valid, error_msg = security.validate_password_strength(user_data.password)
    if not is_valid:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=error_msg
        )
    
    # Check if user already exists
    stmt = select(User).where(User.email == user_data.email)
    result = await db.execute(stmt)
    existing_user = result.scalar_one_or_none()
    
    if existing_user:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Email already registered"
        )
    
    # Create new user
    hashed_password = security.hash_password(user_data.password)
    new_user = User(
        email=user_data.email,
        hashed_password=hashed_password,
        full_name=user_data.full_name,
        is_active=True,
        is_verified=False,  # TODO: Implement email verification
    )
    
    db.add(new_user)
    await db.commit()
    await db.refresh(new_user)
    
    logger.info(f"New user registered: {new_user.email}")
    
    return new_user


@router.post("/login", response_model=TokenResponse)
async def login(
    credentials: UserLogin,
    db: AsyncSession = Depends(get_db)
):
    """
    Login with email and password.
    
    Returns JWT access and refresh tokens.
    """
    # Get user by email
    stmt = select(User).where(User.email == credentials.email)
    result = await db.execute(stmt)
    user = result.scalar_one_or_none()
    
    # Verify user exists and password is correct
    if not user or not security.verify_password(credentials.password, user.hashed_password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect email or password",
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    if not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Inactive user"
        )
    
    # Update last login
    user.last_login = datetime.utcnow()
    await db.commit()
    
    # Create tokens
    access_token = security.create_access_token(data={"sub": user.id})
    refresh_token = security.create_refresh_token(data={"sub": user.id})
    
    logger.info(f"User logged in: {user.email}")
    
    return TokenResponse(
        access_token=access_token,
        refresh_token=refresh_token,
        token_type="bearer"
    )


@router.get("/me", response_model=UserResponse)
async def get_current_user_info(
    current_user: User = Depends(get_current_user)
):
    """Get current user information"""
    return current_user


@router.post("/api-key", response_model=UserWithAPIKey)
async def generate_api_key(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """
    Generate a new API key for programmatic access.
    
    Warning: This will invalidate any existing API key.
    """
    api_key = current_user.generate_api_key()
    await db.commit()
    await db.refresh(current_user)
    
    logger.info(f"API key generated for user: {current_user.email}")
    
    return current_user


@router.delete("/api-key", response_model=MessageResponse)
async def revoke_api_key(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """Revoke current API key"""
    current_user.api_key = None
    await db.commit()
    
    logger.info(f"API key revoked for user: {current_user.email}")
    
    return MessageResponse(message="API key revoked successfully")

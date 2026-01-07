"""URL model for shortened URLs"""

from datetime import datetime
from typing import Optional
from sqlalchemy import String, Boolean, DateTime, Integer, ForeignKey, Index, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.database import Base


class URL(Base):
    """URL model for storing shortened URLs"""
    
    __tablename__ = "urls"
    
    # Primary key
    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    
    # URL data
    short_code: Mapped[str] = mapped_column(
        String(50),
        unique=True,
        index=True,
        nullable=False
    )
    original_url: Mapped[str] = mapped_column(Text, nullable=False)
    
    # Custom alias (optional)
    custom_alias: Mapped[Optional[str]] = mapped_column(
        String(100),
        unique=True,
        index=True,
        nullable=True
    )
    
    # Title and description for link bundles
    title: Mapped[Optional[str]] = mapped_column(String(255), nullable=True)
    description: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    
    # Owner
    user_id: Mapped[Optional[int]] = mapped_column(
        Integer,
        ForeignKey("users.id", ondelete="CASCADE"),
        nullable=True,
        index=True
    )
    
    # Security
    password_hash: Mapped[Optional[str]] = mapped_column(String(255), nullable=True)
    
    # Status
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
    
    # Expiration
    expires_at: Mapped[Optional[datetime]] = mapped_column(DateTime, nullable=True)
    
    # Statistics (denormalized for performance)
    click_count: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
    
    # Timestamps
    created_at: Mapped[datetime] = mapped_column(
        DateTime,
        default=datetime.utcnow,
        nullable=False,
        index=True
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime,
        default=datetime.utcnow,
        onupdate=datetime.utcnow,
        nullable=False
    )
    last_accessed: Mapped[Optional[datetime]] = mapped_column(DateTime, nullable=True)
    
    # Relationships
    owner: Mapped[Optional["User"]] = relationship("User", back_populates="urls")
    analytics: Mapped[list["Analytics"]] = relationship(
        "Analytics",
        back_populates="url",
        cascade="all, delete-orphan"
    )
    
    # Indexes for performance
    __table_args__ = (
        Index("idx_url_short_code", "short_code"),
        Index("idx_url_custom_alias", "custom_alias"),
        Index("idx_url_user_id", "user_id"),
        Index("idx_url_created_at", "created_at"),
        Index("idx_url_user_created", "user_id", "created_at"),
    )
    
    @property
    def is_expired(self) -> bool:
        """Check if URL has expired"""
        if self.expires_at is None:
            return False
        return datetime.utcnow() > self.expires_at
    
    @property
    def is_accessible(self) -> bool:
        """Check if URL is accessible (active and not expired)"""
        return self.is_active and not self.is_expired
    
    def __repr__(self) -> str:
        return f"<URL(id={self.id}, short_code={self.short_code})>"

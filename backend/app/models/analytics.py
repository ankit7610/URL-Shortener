"""Analytics model for click tracking"""

from datetime import datetime
from typing import Optional
from sqlalchemy import String, DateTime, Integer, ForeignKey, Index
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.database import Base


class Analytics(Base):
    """Analytics model for tracking URL clicks"""
    
    __tablename__ = "analytics"
    
    # Primary key
    id: Mapped[int] = mapped_column(primary_key=True, index=True)
    
    # Foreign key to URL
    url_id: Mapped[int] = mapped_column(
        Integer,
        ForeignKey("urls.id", ondelete="CASCADE"),
        nullable=False,
        index=True
    )
    
    # Timestamp
    clicked_at: Mapped[datetime] = mapped_column(
        DateTime,
        default=datetime.utcnow,
        nullable=False,
        index=True
    )
    
    # Geographic data
    ip_address: Mapped[Optional[str]] = mapped_column(String(45), nullable=True)
    country: Mapped[Optional[str]] = mapped_column(String(100), nullable=True)
    city: Mapped[Optional[str]] = mapped_column(String(100), nullable=True)
    
    # Device information
    device_type: Mapped[Optional[str]] = mapped_column(String(50), nullable=True)
    browser: Mapped[Optional[str]] = mapped_column(String(50), nullable=True)
    os: Mapped[Optional[str]] = mapped_column(String(50), nullable=True)
    
    # Referrer
    referrer: Mapped[Optional[str]] = mapped_column(String(500), nullable=True)
    
    # User agent (full string for detailed analysis)
    user_agent: Mapped[Optional[str]] = mapped_column(String(500), nullable=True)
    
    # Relationships
    url: Mapped["URL"] = relationship("URL", back_populates="analytics")
    
    # Indexes for time-series queries
    __table_args__ = (
        Index("idx_analytics_url_id", "url_id"),
        Index("idx_analytics_clicked_at", "clicked_at"),
        Index("idx_analytics_url_time", "url_id", "clicked_at"),
        Index("idx_analytics_country", "country"),
        Index("idx_analytics_device", "device_type"),
    )
    
    def __repr__(self) -> str:
        return f"<Analytics(id={self.id}, url_id={self.url_id}, clicked_at={self.clicked_at})>"


# Note: For production at scale (10M+ clicks/day), consider:
# 1. Table partitioning by date (monthly or weekly)
# 2. Separate read replicas for analytics queries
# 3. Time-series database like TimescaleDB
# 4. Aggregation tables for common queries

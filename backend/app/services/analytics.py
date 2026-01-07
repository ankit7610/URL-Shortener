"""Analytics service for click tracking and aggregation"""

from datetime import datetime, timedelta
from typing import Optional, Dict, List, Any
from sqlalchemy import select, func, and_
from sqlalchemy.ext.asyncio import AsyncSession
from user_agents import parse

from app.models.analytics import Analytics
from app.models.url import URL
from app.utils.logger import logger


class AnalyticsService:
    """Service for tracking and analyzing URL clicks"""
    
    @staticmethod
    async def track_click(
        db: AsyncSession,
        url_id: int,
        ip_address: Optional[str],
        user_agent: Optional[str],
        referrer: Optional[str]
    ) -> Analytics:
        """
        Track a URL click with detailed analytics.
        
        Args:
            db: Database session
            url_id: ID of the URL being clicked
            ip_address: Client IP address
            user_agent: User agent string
            referrer: Referrer URL
        
        Returns:
            Created Analytics record
        """
        # Parse user agent
        device_type = None
        browser = None
        os = None
        
        if user_agent:
            try:
                ua = parse(user_agent)
                device_type = "mobile" if ua.is_mobile else "tablet" if ua.is_tablet else "desktop"
                browser = ua.browser.family
                os = ua.os.family
            except Exception as e:
                logger.warning(f"User agent parsing error: {e}")
        
        # TODO: Add GeoIP lookup for country/city
        # This requires downloading and integrating MaxMind GeoLite2 database
        country = None
        city = None
        
        # Create analytics record
        analytics = Analytics(
            url_id=url_id,
            clicked_at=datetime.utcnow(),
            ip_address=ip_address,
            country=country,
            city=city,
            device_type=device_type,
            browser=browser,
            os=os,
            referrer=referrer,
            user_agent=user_agent
        )
        
        db.add(analytics)
        
        # Increment click count on URL (denormalized for performance)
        stmt = select(URL).where(URL.id == url_id)
        result = await db.execute(stmt)
        url = result.scalar_one_or_none()
        
        if url:
            url.click_count += 1
            url.last_accessed = datetime.utcnow()
        
        await db.commit()
        await db.refresh(analytics)
        
        return analytics
    
    @staticmethod
    async def get_url_analytics(
        db: AsyncSession,
        url_id: int,
        days: int = 30
    ) -> Dict[str, Any]:
        """
        Get aggregated analytics for a URL.
        
        Args:
            db: Database session
            url_id: ID of the URL
            days: Number of days to include in analytics
        
        Returns:
            Dictionary with analytics data
        """
        start_date = datetime.utcnow() - timedelta(days=days)
        
        # Base query
        base_query = select(Analytics).where(
            and_(
                Analytics.url_id == url_id,
                Analytics.clicked_at >= start_date
            )
        )
        
        # Total clicks
        total_clicks_stmt = select(func.count()).select_from(Analytics).where(
            and_(
                Analytics.url_id == url_id,
                Analytics.clicked_at >= start_date
            )
        )
        total_clicks_result = await db.execute(total_clicks_stmt)
        total_clicks = total_clicks_result.scalar()
        
        # Clicks by day
        clicks_by_day_stmt = select(
            func.date(Analytics.clicked_at).label("date"),
            func.count().label("count")
        ).where(
            and_(
                Analytics.url_id == url_id,
                Analytics.clicked_at >= start_date
            )
        ).group_by(func.date(Analytics.clicked_at)).order_by(func.date(Analytics.clicked_at))
        
        clicks_by_day_result = await db.execute(clicks_by_day_stmt)
        clicks_by_day = [
            {"date": str(row.date), "count": row.count}
            for row in clicks_by_day_result
        ]
        
        # Clicks by country
        clicks_by_country_stmt = select(
            Analytics.country,
            func.count().label("count")
        ).where(
            and_(
                Analytics.url_id == url_id,
                Analytics.clicked_at >= start_date,
                Analytics.country.isnot(None)
            )
        ).group_by(Analytics.country).order_by(func.count().desc()).limit(10)
        
        clicks_by_country_result = await db.execute(clicks_by_country_stmt)
        clicks_by_country = [
            {"country": row.country, "count": row.count}
            for row in clicks_by_country_result
        ]
        
        # Clicks by device
        clicks_by_device_stmt = select(
            Analytics.device_type,
            func.count().label("count")
        ).where(
            and_(
                Analytics.url_id == url_id,
                Analytics.clicked_at >= start_date,
                Analytics.device_type.isnot(None)
            )
        ).group_by(Analytics.device_type)
        
        clicks_by_device_result = await db.execute(clicks_by_device_stmt)
        clicks_by_device = [
            {"device": row.device_type, "count": row.count}
            for row in clicks_by_device_result
        ]
        
        # Clicks by browser
        clicks_by_browser_stmt = select(
            Analytics.browser,
            func.count().label("count")
        ).where(
            and_(
                Analytics.url_id == url_id,
                Analytics.clicked_at >= start_date,
                Analytics.browser.isnot(None)
            )
        ).group_by(Analytics.browser).order_by(func.count().desc()).limit(10)
        
        clicks_by_browser_result = await db.execute(clicks_by_browser_stmt)
        clicks_by_browser = [
            {"browser": row.browser, "count": row.count}
            for row in clicks_by_browser_result
        ]
        
        # Top referrers
        top_referrers_stmt = select(
            Analytics.referrer,
            func.count().label("count")
        ).where(
            and_(
                Analytics.url_id == url_id,
                Analytics.clicked_at >= start_date,
                Analytics.referrer.isnot(None)
            )
        ).group_by(Analytics.referrer).order_by(func.count().desc()).limit(10)
        
        top_referrers_result = await db.execute(top_referrers_stmt)
        top_referrers = [
            {"referrer": row.referrer, "count": row.count}
            for row in top_referrers_result
        ]
        
        return {
            "total_clicks": total_clicks,
            "clicks_by_day": clicks_by_day,
            "clicks_by_country": clicks_by_country,
            "clicks_by_device": clicks_by_device,
            "clicks_by_browser": clicks_by_browser,
            "top_referrers": top_referrers,
        }


# Global instance
analytics_service = AnalyticsService()

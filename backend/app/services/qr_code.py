"""QR code generation service"""

import io
from typing import Optional
import qrcode
from qrcode.image.pil import PilImage

from app.core.config import settings
from app.utils.logger import logger


class QRCodeService:
    """Service for generating QR codes"""
    
    @staticmethod
    def generate_qr_code(
        url: str,
        size: int = 300,
        border: int = 2
    ) -> Optional[bytes]:
        """
        Generate QR code for a URL.
        
        Args:
            url: URL to encode in QR code
            size: Size of the QR code in pixels
            border: Border size in boxes
        
        Returns:
            QR code image as bytes (PNG format) or None on error
        """
        try:
            # Create QR code instance
            qr = qrcode.QRCode(
                version=1,  # Auto-adjust size
                error_correction=qrcode.constants.ERROR_CORRECT_L,
                box_size=10,
                border=border,
            )
            
            # Add data
            qr.add_data(url)
            qr.make(fit=True)
            
            # Create image
            img = qr.make_image(fill_color="black", back_color="white")
            
            # Resize to desired size
            img = img.resize((size, size))
            
            # Convert to bytes
            buffer = io.BytesIO()
            img.save(buffer, format="PNG")
            buffer.seek(0)
            
            return buffer.getvalue()
        
        except Exception as e:
            logger.error(f"QR code generation error: {e}")
            return None


# Global instance
qr_service = QRCodeService()

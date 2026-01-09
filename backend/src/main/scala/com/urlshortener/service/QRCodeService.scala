package com.urlshortener.service

import zio.*
import net.glxn.qrgen.javase.QRCode
import net.glxn.qrgen.core.image.ImageType
import java.io.ByteArrayOutputStream

trait QRCodeService:
  def generateQRCode(url: String, size: Int = 300, border: Int = 2): Task[Array[Byte]]

object QRCodeService:
  
  case class Live() extends QRCodeService:
    
    override def generateQRCode(url: String, size: Int = 300, border: Int = 2): Task[Array[Byte]] =
      ZIO.attempt {
        val qrCode = QRCode.from(url)
          .withSize(size, size)
          .withCharset("UTF-8")
          .to(ImageType.PNG)
        
        val stream = new ByteArrayOutputStream()
        qrCode.writeTo(stream)
        stream.toByteArray
      }
  
  val layer: ZLayer[Any, Nothing, QRCodeService] =
    ZLayer.succeed(Live())

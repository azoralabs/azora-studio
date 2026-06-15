package dev.azora.sdk.core.presentation.image

import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import kotlin.math.min

/**
 * Desktop (JVM) implementation of [compressImage] using Java AWT and ImageIO.
 *
 * This implementation uses standard Java image processing APIs:
 * - [ImageIO] for reading and writing images
 * - [BufferedImage] for image manipulation
 * - [Image.getScaledInstance] for resizing with smooth scaling
 * - [ImageWriteParam] for controlling JPEG compression quality
 *
 * ## Processing Steps
 * 1. Read the byte array into a [BufferedImage] using [ImageIO.read]
 * 2. Calculate the scaling ratio to fit within bounds
 * 3. Scale using [Image.SCALE_SMOOTH] for high-quality downsampling
 * 4. Create a new RGB [BufferedImage] from the scaled image
 * 5. Write to JPEG with specified compression quality
 *
 * ## Error Handling
 * If any step fails (unsupported format, no JPEG writer available, etc.),
 * the original byte array is returned unchanged.
 *
 * @see ImageIO For the underlying image I/O operations
 * @see ImageWriteParam.setCompressionQuality For quality control
 */
actual fun ByteArray.compressImage(
    maxWidth: Int,
    maxHeight: Int,
    quality: Int
): ByteArray {
    try {
        // Read the original image
        val inputStream = ByteArrayInputStream(this)
        val originalImage = ImageIO.read(inputStream) ?: return this

        val originalWidth = originalImage.width
        val originalHeight = originalImage.height

        // Calculate new dimensions while maintaining aspect ratio
        val ratio = min(
            maxWidth.toFloat() / originalWidth,
            maxHeight.toFloat() / originalHeight
        )

        val newWidth = (originalWidth * ratio).toInt()
        val newHeight = (originalHeight * ratio).toInt()

        // Resize the image if needed
        val resizedImage = if (ratio < 1.0f) {
            val scaledImage = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH)
            val bufferedImage = BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB)
            val graphics = bufferedImage.createGraphics()
            graphics.drawImage(scaledImage, 0, 0, null)
            graphics.dispose()
            bufferedImage
        } else {
            originalImage
        }

        // Compress to JPEG
        val outputStream = ByteArrayOutputStream()
        val writers = ImageIO.getImageWritersByFormatName("jpg")
        if (!writers.hasNext()) {
            return this
        }

        val writer = writers.next()
        val imageOutputStream = ImageIO.createImageOutputStream(outputStream)
        writer.output = imageOutputStream

        val writeParam = writer.defaultWriteParam
        writeParam.compressionMode = ImageWriteParam.MODE_EXPLICIT
        writeParam.compressionQuality = quality / 100f

        writer.write(null, IIOImage(resizedImage, null, null), writeParam)

        imageOutputStream.close()
        writer.dispose()

        return outputStream.toByteArray()
    } catch (_: Exception) {
        // If compression fails, return original
        return this
    }
}
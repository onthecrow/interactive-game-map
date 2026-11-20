package com.onthecrow.interactivegamemap.map.view

import com.onthecrow.interactivegamemap.map.TileColorMatrix
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toCValues
import kotlin.math.roundToInt
import platform.Accelerate.kvImageNoFlags
import platform.Accelerate.vImageMatrixMultiply_ARGB8888
import platform.Accelerate.vImagePremultiplyData_ARGB8888
import platform.Accelerate.vImageUnpremultiplyData_ARGB8888
import platform.Accelerate.vImage_Buffer
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextCreateImage
import platform.CoreGraphics.CGBitmapContextGetBytesPerRow
import platform.CoreGraphics.CGBitmapContextGetData
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGImageRelease
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIImage

// vImage takes whole numbers: the matrix and the offsets are multiplied by this and the sums divided by it
private const val DIVISOR = 256

// The channels of a pixel in the memory of the bitmap, alpha first, and their rows and columns in a TileColorMatrix
private val MATRIX_CHANNEL_OF_PIXEL_CHANNEL = intArrayOf(3, 0, 1, 2)

/** [colors] as vImage applies them to the pixels of an ARGB bitmap. */
private class PixelColorMatrix(colors: TileColorMatrix) {
    /** Of the pixel's channel j into its channel i at j * 4 + i, the order vImage reads the matrix in. */
    val matrix = ShortArray(TileColorMatrix.CHANNELS * TileColorMatrix.CHANNELS)
    val offsets = IntArray(TileColorMatrix.CHANNELS)

    init {
        for (output in 0 until TileColorMatrix.CHANNELS) {
            val row = MATRIX_CHANNEL_OF_PIXEL_CHANNEL[output] * TileColorMatrix.COLUMNS
            for (input in 0 until TileColorMatrix.CHANNELS) {
                val value = colors.values[row + MATRIX_CHANNEL_OF_PIXEL_CHANNEL[input]]
                matrix[input * TileColorMatrix.CHANNELS + output] = (value * DIVISOR).roundToInt().toShort()
            }
            offsets[output] = (colors.values[row + TileColorMatrix.CHANNELS] * DIVISOR).roundToInt()
        }
    }
}

/**
 * A copy of the image recoloured by [colors], drawn into a bitmap: so it is decoded here and not where the map draws
 * it. Call it off the main thread, a tile takes about a millisecond.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun UIImage.recoloured(colors: TileColorMatrix): UIImage? {
    val image = CGImage ?: return null
    val width = CGImageGetWidth(image)
    val height = CGImageGetHeight(image)
    val colorSpace = CGColorSpaceCreateDeviceRGB()
    // Alpha first and big endian: A, R, G, B in memory, as the ARGB8888 functions of vImage take
    val context = CGBitmapContextCreate(
        data = null,
        width = width,
        height = height,
        bitsPerComponent = 8u,
        bytesPerRow = 0u,
        space = colorSpace,
        bitmapInfo = CGImageAlphaInfo.kCGImageAlphaPremultipliedFirst.value,
    )
    CGColorSpaceRelease(colorSpace)
    context ?: return null

    CGContextDrawImage(context, CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()), image)
    val pixels = PixelColorMatrix(colors)
    memScoped {
        val buffer = alloc<vImage_Buffer>().apply {
            data = CGBitmapContextGetData(context)
            this.width = width.convert()
            this.height = height.convert()
            rowBytes = CGBitmapContextGetBytesPerRow(context)
        }
        // The matrix is for colors without their alpha multiplied in, as on Android
        vImageUnpremultiplyData_ARGB8888(buffer.ptr, buffer.ptr, kvImageNoFlags.convert())
        vImageMatrixMultiply_ARGB8888(
            buffer.ptr,
            buffer.ptr,
            pixels.matrix.toCValues(),
            DIVISOR,
            null,
            pixels.offsets.toCValues(),
            kvImageNoFlags.convert(),
        )
        vImagePremultiplyData_ARGB8888(buffer.ptr, buffer.ptr, kvImageNoFlags.convert())
    }
    val result = CGBitmapContextCreateImage(context)
    CGContextRelease(context)
    result ?: return null
    return UIImage.imageWithCGImage(result).also { CGImageRelease(result) }
}

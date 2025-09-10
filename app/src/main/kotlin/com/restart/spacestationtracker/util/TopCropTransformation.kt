package com.restart.spacestationtracker.util

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.core.graphics.applyCanvas
import coil.size.Dimension
import coil.size.Size
import coil.transform.Transformation
import kotlin.math.max
import androidx.core.graphics.createBitmap

class TopCropTransformation : Transformation {

    override val cacheKey: String = TopCropTransformation::class.java.name

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        val inputWidth = input.width
        val inputHeight = input.height

        val outputWidth = (size.width as? Dimension.Pixels)?.px ?: inputWidth
        val outputHeight = (size.height as? Dimension.Pixels)?.px ?: inputHeight

        if (inputWidth == outputWidth && inputHeight == outputHeight) {
            return input
        }

        val scale = max(outputWidth.toFloat() / inputWidth, outputHeight.toFloat() / inputHeight)
        val scaledWidth = scale * inputWidth
        val scaledHeight = scale * inputHeight
        val left = (outputWidth - scaledWidth) / 2f
        val top = 0f

        val output = createBitmap(outputWidth, outputHeight)
        output.applyCanvas {
            translate(left, top)
            scale(scale, scale)
            drawBitmap(input, 0f, 0f, paint)
        }

        return output
    }
}

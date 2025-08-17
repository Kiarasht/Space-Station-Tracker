package com.restart.spacestationtracker.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest
import kotlin.math.max
import kotlin.math.roundToInt

class TopCrop : BitmapTransformation() {

    override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
        val width = toTransform.width
        val height = toTransform.height

        val scale = max(outWidth.toFloat() / width, outHeight.toFloat() / height)
        val scaledWidth = scale * width

        val dx = (outWidth - scaledWidth) / 2f
        val dy = 0f

        val matrix = Matrix()
        matrix.setScale(scale, scale)
        matrix.postTranslate(dx.roundToInt().toFloat(), dy)

        val result = pool.get(outWidth, outHeight, getNonNullConfig(toTransform))
        result.setHasAlpha(toTransform.hasAlpha())

        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(toTransform, matrix, paint)
        return result
    }

    private fun getNonNullConfig(bitmap: Bitmap): Bitmap.Config {
        return bitmap.config ?: Bitmap.Config.ARGB_8888
    }

    override fun equals(other: Any?) = other is TopCrop

    override fun hashCode() = ID.hashCode()

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(ID_BYTES)
    }

    private companion object {
        private const val ID = "com.restart.spacestationtracker.adapter.TopCrop"
        private val ID_BYTES = ID.toByteArray(Charsets.UTF_8)
    }
}
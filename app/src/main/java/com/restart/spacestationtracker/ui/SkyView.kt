package com.restart.spacestationtracker.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import com.restart.spacestationtracker.R
import kotlin.math.cos
import kotlin.math.sin

class SkyPathView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var startCompass = "S"
    private var endCompass = "N"
    private var maxElevation = 0f

    private val horizonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(2f)
        color = ContextCompat.getColor(context, R.color.gray600)
    }

    private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(2f)
        color = ContextCompat.getColor(context, R.color.colorPrimary)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = spToPx(12f)
        color = ContextCompat.getColor(context, R.color.gray600)
    }

    private val userTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = spToPx(16f)
        color = ContextCompat.getColor(context, R.color.material_on_surface_emphasis_medium)
        textAlign = Paint.Align.CENTER
    }

    private val arcBounds = RectF()
    private val userIcon: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_man)
    private val issIcon: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_iss)

    fun setPath(startCompass: String, endCompass: String, maxElevation: Float) {
        this.startCompass = startCompass
        this.endCompass = endCompass
        this.maxElevation = maxElevation.coerceIn(0f, 90f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val horizonY = viewHeight - textPaint.fontMetrics.descent
        val arcCenterX = viewWidth / 2f
        val arcCenterY = horizonY

        val radiusFromWidth = (viewWidth - paddingStart - paddingEnd) / 2f

        val topClearance = dpToPx(32f)
        val radiusFromHeight = arcCenterY - paddingTop - topClearance

        val arcRadius = radiusFromWidth.coerceAtMost(radiusFromHeight).coerceAtLeast(0f)

        if (arcRadius <= 0) {
            return
        }

        val horizonStart = arcCenterX - arcRadius
        val horizonEnd = arcCenterX + arcRadius
        canvas.drawLine(horizonStart, horizonY, horizonEnd, horizonY, horizonPaint)

        userIcon?.let { icon ->
            val iconSize = dpToPx(32f).toInt()
            val iconLeft = (arcCenterX - iconSize / 2).toInt()
            val iconTop = (horizonY - iconSize).toInt()
            val iconRight = (arcCenterX + iconSize / 2).toInt()
            val iconBottom = horizonY.toInt()

            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
            icon.draw(canvas)

            val textY = iconTop - dpToPx(4f)
            canvas.drawText("You", arcCenterX, textY, userTextPaint)
        }

        val compassLabelOffset = dpToPx(8f)
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(
            startCompass,
            arcCenterX - arcRadius + compassLabelOffset,
            horizonY - dpToPx(8f),
            textPaint
        )

        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(
            endCompass,
            arcCenterX + arcRadius - compassLabelOffset,
            horizonY - dpToPx(8f),
            textPaint
        )

        arcBounds.set(
            arcCenterX - arcRadius,
            arcCenterY - arcRadius,
            arcCenterX + arcRadius,
            arcCenterY + arcRadius
        )
        canvas.drawArc(arcBounds, 180f, 180f, false, pathPaint)

        val angleInDegrees = 180.0 + maxElevation
        val angleInRadians = Math.toRadians(angleInDegrees)

        val indicatorX = arcCenterX + (arcRadius * cos(angleInRadians)).toFloat()
        val indicatorY = arcCenterY + (arcRadius * sin(angleInRadians)).toFloat()

        issIcon?.let { icon ->
            val iconSize = dpToPx(32f).toInt()
            val iconLeft = (indicatorX - iconSize / 2).toInt()
            val iconTop = (indicatorY - iconSize / 2).toInt()
            val iconRight = (indicatorX + iconSize / 2).toInt()
            val iconBottom = (indicatorY + iconSize / 2).toInt()
            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
            icon.draw(canvas)
        }

        textPaint.textAlign = Paint.Align.CENTER
        val iconRadius = dpToPx(16f)
        val textX = indicatorX
        val textY = indicatorY - iconRadius - textPaint.fontMetrics.descent
        canvas.drawText("${maxElevation.toInt()}°", textX, textY, textPaint)
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)
    }

    private fun spToPx(sp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics)
    }
}
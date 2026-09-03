package com.iykyk.facestory.data.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.iykyk.facestory.domain.model.PersonCluster
import com.iykyk.facestory.util.BitmapUtils

class CollageRenderer {

    private val width = 1080
    private val height = 1920

    fun render(people: List<PersonCluster>): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                Color.parseColor("#0F172A"), // Slate 900
                Color.parseColor("#020617"), // Slate 950
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#818CF8")
            textSize = 56f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("FACESTORY", width / 2f, 180f, brandPaint)

        val totalAppearances = people.sumOf { it.appearanceCount }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 34f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("${people.size} PEOPLE  •  $totalAppearances APPEARANCES", width / 2f, 240f, subtitlePaint)

        val contentTop = 320f
        val contentBottom = 1750f
        val contentHeight = contentBottom - contentTop
        val contentWidth = width - 120f

        val count = people.size.coerceAtMost(6)
        if (count > 0) {
            val (cols, rows) = when {
                count <= 1 -> 1 to 1
                count <= 2 -> 1 to 2
                count <= 4 -> 2 to 2
                else -> 2 to 3
            }

            val cellGap = 32f
            val cellW = (contentWidth - (cols - 1) * cellGap) / cols
            val cellH = (contentHeight - (rows - 1) * cellGap) / rows

            for (i in 0 until count) {
                val col = i % cols
                val row = i / cols
                val left = 60f + col * (cellW + cellGap)
                val top = contentTop + row * (cellH + cellGap)
                val rect = RectF(left, top, left + cellW, top + cellH)

                drawPersonCard(canvas, rect, people[i], i + 1)
            }
        }

        return bitmap
    }

    private fun drawPersonCard(canvas: Canvas, rect: RectF, person: PersonCluster, personIndex: Int) {
        val repFace = person.representativeFace ?: person.faces.firstOrNull() ?: return

        val faceCrop = BitmapUtils.getExpandedFaceCrop(
            repFace.frame,
            repFace.boundingBox,
            expansionFactor = 1.9f
        )

        val cardRadius = 36f
        val roundedPhoto = getRoundedCroppedBitmap(faceCrop, rect.width().toInt(), rect.height().toInt(), cardRadius)
        canvas.drawBitmap(roundedPhoto, rect.left, rect.top, null)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = Color.parseColor("#334155")
        }
        canvas.drawRoundRect(rect, cardRadius, cardRadius, borderPaint)

        val badgeHeight = 64f
        val badgeRect = RectF(rect.left + 24f, rect.bottom - badgeHeight - 24f, rect.right - 24f, rect.bottom - 24f)

        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(200, 15, 23, 42) // Dark glass overlay
        }
        canvas.drawRoundRect(badgeRect, 20f, 20f, badgeBgPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val textY = badgeRect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText("Person $personIndex  •  ${person.appearanceCount} appearances", badgeRect.centerX(), textY, textPaint)
    }

    private fun getRoundedCroppedBitmap(src: Bitmap, targetW: Int, targetH: Int, radius: Float): Bitmap {
        val output = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rectF = RectF(0f, 0f, targetW.toFloat(), targetH.toFloat())
        canvas.drawRoundRect(rectF, radius, radius, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

        val srcRatio = src.width.toFloat() / src.height.toFloat()
        val targetRatio = targetW.toFloat() / targetH.toFloat()

        val srcRect = if (srcRatio > targetRatio) {
            val newW = (src.height * targetRatio).toInt()
            val offset = (src.width - newW) / 2
            Rect(offset, 0, offset + newW, src.height)
        } else {
            val newH = (src.width / targetRatio).toInt()
            val offset = (src.height - newH) / 2
            Rect(0, offset, src.width, offset + newH)
        }

        canvas.drawBitmap(src, srcRect, Rect(0, 0, targetW, targetH), paint)
        return output
    }
}

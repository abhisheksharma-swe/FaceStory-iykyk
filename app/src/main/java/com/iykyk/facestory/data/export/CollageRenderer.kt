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
                Color.parseColor("#0B0F19"),
                Color.parseColor("#020617"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#818CF8")
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.08f
        }
        canvas.drawText("FACESTORY", width / 2f, 150f, brandPaint)

        val totalAppearances = people.sumOf { it.appearanceCount }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.04f
        }
        canvas.drawText("${people.size} PEOPLE DETECTED  •  $totalAppearances APPEARANCES", width / 2f, 205f, subtitlePaint)

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#312E81")
            strokeWidth = 2f
        }
        canvas.drawLine(140f, 235f, width - 140f, 235f, linePaint)

        val count = people.size
        if (count > 0) {
            val contentTop = 265f
            val contentBottom = 1820f
            val contentHeight = contentBottom - contentTop
            val contentWidth = width - 80f

            val (cols, rows) = when {
                count == 1 -> 1 to 1
                count == 2 -> 1 to 2
                count <= 4 -> 2 to 2
                count <= 6 -> 2 to 3
                count <= 8 -> 2 to 4
                count <= 9 -> 3 to 3
                count <= 12 -> 3 to 4
                count <= 15 -> 3 to 5
                count <= 16 -> 4 to 4
                count <= 20 -> 4 to 5
                else -> {
                    val c = Math.ceil(Math.sqrt(count.toDouble())).toInt().coerceAtMost(5)
                    val r = Math.ceil(count.toDouble() / c).toInt()
                    c to r
                }
            }

            val cellGap = if (rows >= 4 || cols >= 3) 18f else 26f
            val cellW = (contentWidth - (cols - 1) * cellGap) / cols
            val cellH = (contentHeight - (rows - 1) * cellGap) / rows
            val cardRadius = (if (rows >= 4 || cols >= 3) 24f else 32f).coerceAtMost(cellW * 0.12f)

            for (i in 0 until count) {
                val col = i % cols
                val row = i / cols
                val left = 40f + col * (cellW + cellGap)
                val top = contentTop + row * (cellH + cellGap)
                val rect = RectF(left, top, left + cellW, top + cellH)

                drawPersonCard(canvas, rect, people[i], i + 1, cardRadius, cols, rows)
            }
        }

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.05f
        }
        canvas.drawText("FACESTORY AI • PORTRAIT COLLAGE", width / 2f, 1880f, footerPaint)

        return bitmap
    }

    private fun drawPersonCard(
        canvas: Canvas,
        rect: RectF,
        person: PersonCluster,
        personIndex: Int,
        cardRadius: Float,
        totalCols: Int,
        totalRows: Int
    ) {
        val repFace = person.representativeFace ?: person.faces.firstOrNull() ?: return

        val cardW = rect.width().toInt().coerceAtLeast(1)
        val cardH = rect.height().toInt().coerceAtLeast(1)
        val portraitBitmap = BitmapUtils.getPortraitCropForCollage(
            repFace.frame,
            repFace.boundingBox,
            cardW,
            cardH
        )

        val roundedPhoto = getRoundedCroppedBitmap(portraitBitmap, cardW, cardH, cardRadius)
        val drawPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(roundedPhoto, rect.left, rect.top, drawPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = if (totalRows >= 4) 2.5f else 3.5f
            color = Color.parseColor("#334155")
        }
        canvas.drawRoundRect(rect, cardRadius, cardRadius, borderPaint)

        val badgeHeight = (if (totalRows >= 4 || totalCols >= 3) 48f else 60f).coerceAtMost(rect.height() * 0.22f)
        val badgeMargin = if (totalRows >= 4) 12f else 18f
        val badgeRect = RectF(
            rect.left + badgeMargin,
            rect.bottom - badgeHeight - badgeMargin,
            rect.right - badgeMargin,
            rect.bottom - badgeMargin
        )

        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(215, 15, 23, 42)
        }
        canvas.drawRoundRect(badgeRect, badgeHeight / 2f, badgeHeight / 2f, badgeBgPaint)

        val badgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            color = Color.argb(100, 129, 140, 248)
        }
        canvas.drawRoundRect(badgeRect, badgeHeight / 2f, badgeHeight / 2f, badgeBorderPaint)

        val textSize = (if (totalRows >= 4 || totalCols >= 3) 22f else 26f).coerceAtMost(badgeHeight * 0.48f)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            this.textSize = textSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val textY = badgeRect.centerY() - (textPaint.descent() + textPaint.ascent()) / 2
        val label = if (totalCols >= 3) "P$personIndex • ${person.appearanceCount}x" else "Person $personIndex  •  ${person.appearanceCount} appearances"
        canvas.drawText(label, badgeRect.centerX(), textY, textPaint)
    }

    private fun getRoundedCroppedBitmap(src: Bitmap, targetW: Int, targetH: Int, radius: Float): Bitmap {
        val safeW = targetW.coerceAtLeast(1)
        val safeH = targetH.coerceAtLeast(1)
        val output = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rectF = RectF(0f, 0f, safeW.toFloat(), safeH.toFloat())
        canvas.drawRoundRect(rectF, radius, radius, paint)

        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

        val srcRatio = src.width.toFloat() / src.height.toFloat()
        val targetRatio = safeW.toFloat() / safeH.toFloat()

        val srcRect = if (srcRatio > targetRatio) {
            val newW = (src.height * targetRatio).toInt().coerceAtLeast(1)
            val offset = (src.width - newW) / 2
            Rect(offset, 0, (offset + newW).coerceAtMost(src.width), src.height)
        } else {
            val newH = (src.width / targetRatio).toInt().coerceAtLeast(1)
            val offset = (src.height - newH) / 2
            Rect(0, offset, src.width, (offset + newH).coerceAtMost(src.height))
        }

        canvas.drawBitmap(src, srcRect, Rect(0, 0, safeW, safeH), paint)
        return output
    }
}

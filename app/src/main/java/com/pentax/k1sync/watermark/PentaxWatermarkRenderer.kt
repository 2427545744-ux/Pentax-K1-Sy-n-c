package com.pentax.k1sync.watermark

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Renders 3 signature Pentax K-1 photo watermark frames:
 * 1. MINIMAL_WHITE: Clean white bottom border, left camera name + Pentax accent block, right single-line EXIF.
 * 2. BLUR_FRAME: Blurred photo ambient background, rounded corners + drop shadow photo card, centered PENTAX emblem & EXIF.
 * 3. CLASSIC_DIVIDER: Clean white bottom border, left camera/lens title, right PENTAX emblem | vertical divider | 2-line EXIF & timestamp.
 */
object PentaxWatermarkRenderer {

    suspend fun render(
        source: Bitmap,
        config: WatermarkConfig,
        maxLongEdge: Int? = null
    ): Bitmap = withContext(Dispatchers.Default) {
        val working = if (maxLongEdge != null && (source.width > maxLongEdge || source.height > maxLongEdge)) {
            val scale = maxLongEdge.toFloat() / maxOf(source.width, source.height)
            Bitmap.createScaledBitmap(
                source,
                (source.width * scale).roundToInt().coerceAtLeast(1),
                (source.height * scale).roundToInt().coerceAtLeast(1),
                true
            )
        } else {
            source
        }

        val result = when (config.style) {
            WatermarkStyle.MINIMAL_WHITE -> renderMinimalWhite(working, config)
            WatermarkStyle.BLUR_FRAME -> renderBlurFrame(working, config)
            WatermarkStyle.CLASSIC_DIVIDER -> renderClassicDivider(working, config)
        }

        if (working !== source) {
            working.recycle()
        }
        result
    }

    /**
     * Style 1: Minimal White Gallery Border
     */
    private fun renderMinimalWhite(src: Bitmap, cfg: WatermarkConfig): Bitmap {
        val w = src.width
        val h = src.height
        val shortEdge = min(w, h).toFloat()
        val barH = (shortEdge * 0.135f).roundToInt().coerceAtLeast(72)
        val out = Bitmap.createBitmap(w, h + barH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        // Draw white base & photo
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(src, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))

        // Optional subtle photo-bottom overlay signature (matches reference image 1)
        if (cfg.showOverlaySignature && cfg.signature.isNotBlank()) {
            val ovPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(225, 255, 255, 255)
                textSize = shortEdge * 0.025f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                setShadowLayer(shortEdge * 0.008f, 0f, shortEdge * 0.003f, Color.argb(180, 0, 0, 0))
            }
            val text = "${cfg.cameraTitle}  ·  ${cfg.signature}"
            canvas.drawText(text, w / 2f, h - shortEdge * 0.028f, ovPaint)
        }

        val padX = w * 0.048f
        val barCenterY = h + barH / 2f

        // Left: Camera Model + Pentax accent square block
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(20, 20, 20)
            textSize = barH * 0.30f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.03f
        }
        val titleText = cfg.cameraTitle.ifBlank { "PENTAX K-1" }
        val titleBaseline = barCenterY - (titlePaint.descent() + titlePaint.ascent()) / 2f
        canvas.drawText(titleText, padX, titleBaseline, titlePaint)

        val titleWidth = titlePaint.measureText(titleText)
        val sqSize = barH * 0.115f
        val sqLeft = padX + titleWidth + barH * 0.13f
        val sqBottom = titleBaseline - barH * 0.01f
        val sqPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cfg.accentColor.colorInt
        }
        canvas.drawRoundRect(
            RectF(sqLeft, sqBottom - sqSize, sqLeft + sqSize, sqBottom),
            sqSize * 0.18f, sqSize * 0.18f, sqPaint
        )

        // Optional lens subtitle beneath title if enabled and non-empty
        if (cfg.showLensName && cfg.lensTitle.isNotBlank()) {
            val lensPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(120, 120, 120)
                textSize = barH * 0.145f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            canvas.drawText(cfg.lensTitle, padX, titleBaseline + barH * 0.21f, lensPaint)
        }

        // Right: Single-line crisp EXIF
        val exifPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(28, 28, 28)
            textSize = barH * 0.225f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
            letterSpacing = 0.01f
        }
        val exifLine = cfg.buildExifLine(uppercaseF = false)
        val exifBaseline = barCenterY - (exifPaint.descent() + exifPaint.ascent()) / 2f
        canvas.drawText(exifLine, w - padX, exifBaseline, exifPaint)

        if (cfg.showDateTime && cfg.dateTime.isNotBlank()) {
            val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(142, 142, 142)
                textSize = barH * 0.14f
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(cfg.dateTime, w - padX, exifBaseline + barH * 0.21f, datePaint)
        }

        return out
    }

    /**
     * Style 2: Ambient Blurred Frame with Rounded Photo Card & Drop Shadow
     */
    private fun renderBlurFrame(src: Bitmap, cfg: WatermarkConfig): Bitmap {
        val w = src.width
        val h = src.height
        val shortEdge = min(w, h).toFloat()

        val padX = (shortEdge * 0.088f).roundToInt()
        val padTop = (shortEdge * 0.072f).roundToInt()
        val padBottom = (shortEdge * 0.178f).roundToInt().coerceAtLeast(92)

        val outW = w + padX * 2
        val outH = h + padTop + padBottom
        val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        // 1. Create silky blurred background from downscaled copy
        val blurThumbW = 240
        val blurThumbH = (blurThumbW * (outH.toFloat() / outW)).roundToInt().coerceIn(120, 420)
        val tiny = Bitmap.createScaledBitmap(src, blurThumbW, blurThumbH, true)
        val blurred = FastStackBlur.blur(tiny, radius = 26)
        tiny.recycle()

        val bgPaint = Paint(Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(blurred, null, Rect(0, 0, outW, outH), bgPaint)
        blurred.recycle()

        // 2. Subtle atmospheric bottom scrim for crisp white typography readability
        val scrimPaint = Paint().apply {
            shader = LinearGradient(
                0f, outH * 0.62f, 0f, outH.toFloat(),
                intArrayOf(Color.TRANSPARENT, Color.argb(128, 8, 10, 14)),
                null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, outW.toFloat(), outH.toFloat(), scrimPaint)

        // 3. Draw soft elevation shadow behind rounded foreground photo
        val photoRect = RectF(
            padX.toFloat(),
            padTop.toFloat(),
            (padX + w).toFloat(),
            (padTop + h).toFloat()
        )
        val cornerRadius = shortEdge * 0.024f
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(230, 12, 12, 14)
            setShadowLayer(
                shortEdge * 0.036f,
                0f,
                shortEdge * 0.016f,
                Color.argb(190, 0, 0, 0)
            )
        }
        canvas.drawRoundRect(photoRect, cornerRadius, cornerRadius, shadowPaint)

        // 4. Clip & draw sharp rounded photo
        val saveCount = canvas.save()
        val clipPath = Path().apply {
            addRoundRect(photoRect, cornerRadius, cornerRadius, Path.Direction.CW)
        }
        canvas.clipPath(clipPath)
        canvas.drawBitmap(src, photoRect.left, photoRect.top, Paint(Paint.FILTER_BITMAP_FLAG))
        canvas.restoreToCount(saveCount)

        // 5. Centered bottom Pentax branding & EXIF
        val footerTop = padTop + h
        val centerX = outW / 2f

        val logoText = "PENTAX"
        val modelText = cfg.cameraTitle.ifBlank { "PENTAX K-1" }

        val logoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = padBottom * 0.27f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD_ITALIC)
            letterSpacing = 0.06f
            setShadowLayer(padBottom * 0.06f, 0f, padBottom * 0.02f, Color.argb(160, 0, 0, 0))
        }
        val modelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(235, 255, 255, 255)
            textSize = padBottom * 0.21f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            letterSpacing = 0.04f
            setShadowLayer(padBottom * 0.05f, 0f, padBottom * 0.02f, Color.argb(150, 0, 0, 0))
        }

        val logoW = logoPaint.measureText(logoText)
        val modelW = modelPaint.measureText(modelText)
        val gap = padBottom * 0.14f
        val totalLine1W = logoW + gap + modelW
        val startX = centerX - totalLine1W / 2f
        val line1Y = footerTop + padBottom * 0.48f

        canvas.drawText(logoText, startX, line1Y, logoPaint)
        canvas.drawText(modelText, startX + logoW + gap, line1Y, modelPaint)

        // Subtle Pentax accent dash under PENTAX logo
        val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cfg.accentColor.colorInt
            strokeWidth = (padBottom * 0.025f).coerceAtLeast(2f)
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(
            startX + logoW * 0.05f,
            line1Y + padBottom * 0.065f,
            startX + logoW * 0.92f,
            line1Y + padBottom * 0.065f,
            dashPaint
        )

        // Line 2: EXIF + optional signature/date
        val exifPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(215, 240, 244, 248)
            textSize = padBottom * 0.165f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.03f
            setShadowLayer(padBottom * 0.04f, 0f, padBottom * 0.015f, Color.argb(150, 0, 0, 0))
        }
        val extra = buildString {
            append(cfg.buildExifLine(uppercaseF = true))
            if (cfg.signature.isNotBlank()) {
                append("   ·   ")
                append(cfg.signature)
            } else if (cfg.showDateTime && cfg.dateTime.isNotBlank()) {
                append("   ·   ")
                append(cfg.dateTime.substringBefore(" "))
            }
        }
        canvas.drawText(extra, centerX, footerTop + padBottom * 0.78f, exifPaint)

        return out
    }

    /**
     * Style 3: Classic Dual-Column Divider Border (PENTAX Logo | EXIF & Date)
     */
    private fun renderClassicDivider(src: Bitmap, cfg: WatermarkConfig): Bitmap {
        val w = src.width
        val h = src.height
        val shortEdge = min(w, h).toFloat()
        val barH = (shortEdge * 0.155f).roundToInt().coerceAtLeast(88)
        val out = Bitmap.createBitmap(w, h + barH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)

        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(src, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))

        val padX = w * 0.045f
        val barTop = h.toFloat()
        val barCenterY = barTop + barH / 2f

        // Left column: Camera model (+ optional lens name below)
        val hasLens = cfg.showLensName && cfg.lensTitle.isNotBlank()
        val cameraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(22, 22, 22)
            textSize = barH * 0.255f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.04f
        }
        val modelStr = cfg.cameraTitle.ifBlank { "PENTAX K-1" }
        if (hasLens) {
            canvas.drawText(modelStr, padX, barCenterY - barH * 0.03f, cameraPaint)
            val lensPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(128, 128, 128)
                textSize = barH * 0.155f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            }
            canvas.drawText(cfg.lensTitle, padX, barCenterY + barH * 0.22f, lensPaint)
        } else {
            val baseY = barCenterY - (cameraPaint.descent() + cameraPaint.ascent()) / 2f
            canvas.drawText(modelStr, padX, baseY, cameraPaint)
        }

        // Right column: Top line EXIF, Bottom line Date/Signature
        val exifTopPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(22, 22, 22)
            textSize = barH * 0.215f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.01f
        }
        val dateSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(138, 138, 138)
            textSize = barH * 0.165f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            letterSpacing = 0.02f
        }

        val exifStr = cfg.buildExifLine(uppercaseF = false)
        val subStr = buildString {
            if (cfg.showDateTime && cfg.dateTime.isNotBlank()) append(cfg.dateTime)
            if (cfg.signature.isNotBlank()) {
                if (isNotEmpty()) append("  ·  ")
                append(cfg.signature)
            }
        }

        val rightBlockW = maxOf(
            exifTopPaint.measureText(exifStr),
            if (subStr.isNotBlank()) dateSubPaint.measureText(subStr) else 0f
        )
        val rightBlockLeft = (w - padX - rightBlockW).coerceAtLeast(w * 0.52f)

        if (subStr.isNotBlank()) {
            canvas.drawText(exifStr, rightBlockLeft, barCenterY - barH * 0.04f, exifTopPaint)
            canvas.drawText(subStr, rightBlockLeft, barCenterY + barH * 0.23f, dateSubPaint)
        } else {
            val baseY = barCenterY - (exifTopPaint.descent() + exifTopPaint.ascent()) / 2f
            canvas.drawText(exifStr, rightBlockLeft, baseY, exifTopPaint)
        }

        // Thin vertical divider line
        val dividerX = rightBlockLeft - barH * 0.22f
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(212, 212, 212)
            strokeWidth = (barH * 0.014f).coerceAtLeast(1.5f)
        }
        canvas.drawLine(
            dividerX,
            barCenterY - barH * 0.26f,
            dividerX,
            barCenterY + barH * 0.26f,
            dividerPaint
        )

        // Classic PENTAX Logotype to the left of the vertical divider
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (cfg.accentColor == PentaxAccentColor.CLASSIC_BLACK) {
                Color.rgb(18, 18, 18)
            } else {
                cfg.accentColor.colorInt
            }
            textSize = barH * 0.275f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            letterSpacing = 0.08f
            textAlign = Paint.Align.RIGHT
        }
        val brandX = dividerX - barH * 0.22f
        val brandY = barCenterY - (brandPaint.descent() + brandPaint.ascent()) / 2f
        canvas.drawText("PENTAX", brandX, brandY, brandPaint)

        return out
    }
}

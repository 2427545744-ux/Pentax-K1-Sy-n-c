package com.pentax.k1sync.watermark

import android.graphics.Color
import com.pentax.k1sync.model.PhotoExif

enum class WatermarkStyle(val title: String, val subtitle: String) {
    MINIMAL_WHITE("极简白底风", "纯白底边 · 左机型色块 · 右单行参数"),
    BLUR_FRAME("毛玻璃氛围风", "原图高斯虚化底 · 圆角阴影画框 · 居中铭牌"),
    CLASSIC_DIVIDER("经典双栏风", "纯白底边 · PENTAX 徽标与竖线双行参数")
}

enum class PentaxAccentColor(val label: String, val colorInt: Int) {
    PENTAX_RED("宾得红", Color.rgb(217, 30, 42)),
    PENTAX_GREEN("星标绿", Color.rgb(46, 191, 109)),
    CLASSIC_BLACK("经典黑", Color.rgb(24, 24, 24))
}

data class WatermarkConfig(
    val style: WatermarkStyle = WatermarkStyle.CLASSIC_DIVIDER,
    val accentColor: PentaxAccentColor = PentaxAccentColor.PENTAX_RED,
    val cameraTitle: String = "PENTAX K-1",
    val lensTitle: String = "",
    val focalLength: String = "77mm",
    val aperture: String = "1.8",
    val shutterSpeed: String = "1/500s",
    val iso: String = "100",
    val dateTime: String = "2026.11.22 17:42:18",
    val signature: String = "",
    val showDateTime: Boolean = true,
    val showOverlaySignature: Boolean = false,
    val showLensName: Boolean = true,
    val highQualityExport: Boolean = true
) {
    fun buildExifLine(uppercaseF: Boolean = false): String {
        val parts = mutableListOf<String>()
        if (focalLength.isNotBlank()) {
            val f = focalLength.trim()
            parts += if (f.endsWith("mm", ignoreCase = true)) f else "${f}mm"
        }
        if (aperture.isNotBlank()) {
            val a = aperture.trim()
                .removePrefix("f/").removePrefix("F/")
                .removePrefix("f").removePrefix("F")
            parts += if (uppercaseF) "F$a" else "f/$a"
        }
        if (shutterSpeed.isNotBlank()) {
            val s = shutterSpeed.trim().let { raw ->
                if (raw.startsWith("1.") && !raw.contains("/")) "1/${raw.removePrefix("1.")}" else raw
            }
            parts += if (s.endsWith("s", ignoreCase = true)) s else "${s}s"
        }
        if (iso.isNotBlank()) {
            val i = iso.trim().removePrefix("ISO").removePrefix("iso").trim()
            parts += "ISO$i"
        }
        return parts.joinToString("  ")
    }

    companion object {
        fun fromExif(exif: PhotoExif, base: WatermarkConfig = WatermarkConfig()): WatermarkConfig {
            return base.copy(
                cameraTitle = exif.cameraModel.ifBlank { "PENTAX K-1" },
                lensTitle = exif.lensModel.ifBlank { base.lensTitle },
                focalLength = exif.focalLength.ifBlank { base.focalLength },
                aperture = exif.aperture.ifBlank { base.aperture },
                shutterSpeed = exif.shutterSpeed.ifBlank { base.shutterSpeed },
                iso = exif.iso.ifBlank { base.iso },
                dateTime = exif.dateTime.ifBlank { base.dateTime }
            )
        }
    }
}

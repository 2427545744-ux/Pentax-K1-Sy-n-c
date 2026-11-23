package com.pentax.k1sync.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pentax.k1sync.model.CameraInfo
import com.pentax.k1sync.model.HistogramData
import com.pentax.k1sync.ui.theme.DarkroomBorder
import com.pentax.k1sync.ui.theme.DarkroomCard
import com.pentax.k1sync.ui.theme.PentaxGreen
import com.pentax.k1sync.ui.theme.PentaxRed
import com.pentax.k1sync.ui.theme.TextPrimary
import com.pentax.k1sync.ui.theme.TextSecondary

@Composable
fun ConnectionStatusBadge(cameraInfo: CameraInfo, modifier: Modifier = Modifier) {
    val dotColor = when {
        cameraInfo.connected && cameraInfo.isMock -> Color(0xFFFFB74D)
        cameraInfo.connected -> PentaxGreen
        else -> PentaxRed
    }
    val text = when {
        cameraInfo.connected && cameraInfo.isMock -> "模拟 K-1 · ${cameraInfo.batteryPercent}%"
        cameraInfo.connected -> "${cameraInfo.model} · ${cameraInfo.batteryPercent}%"
        else -> "未连接 K-1"
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(DarkroomCard)
            .border(1.dp, DarkroomBorder, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun RgbHistogramView(
    histogram: HistogramData,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xB80A0A0C))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
            .padding(4.dp)
    ) {
        val bins = histogram.luma.size
        if (bins < 2) return@Canvas
        val stepX = size.width / (bins - 1)

        fun drawChannel(values: FloatArray, color: Color) {
            val path = Path().apply {
                moveTo(0f, size.height)
                for (i in 0 until bins) {
                    val x = i * stepX
                    val y = size.height * (1f - values[i].coerceIn(0f, 1f))
                    lineTo(x, y)
                }
                lineTo(size.width, size.height)
                close()
            }
            drawPath(path, color = color)
        }

        drawChannel(histogram.red, Color(0x66FF4B4B))
        drawChannel(histogram.green, Color(0x662EBF6D))
        drawChannel(histogram.blue, Color(0x664DA6FF))
        drawChannel(histogram.luma, Color(0x77FFFFFF))
    }
}

@Composable
fun SectionHeader(title: String, trailing: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (!trailing.isNullOrBlank()) {
            Text(
                text = trailing,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

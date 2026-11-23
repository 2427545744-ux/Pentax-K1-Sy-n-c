package com.pentax.k1sync.ui.remote

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pentax.k1sync.model.GridOverlayMode
import com.pentax.k1sync.model.RemoteShotMode
import com.pentax.k1sync.model.ShootingParams
import com.pentax.k1sync.ui.components.ConnectionStatusBadge
import com.pentax.k1sync.ui.theme.DarkroomBg
import com.pentax.k1sync.ui.theme.DarkroomBorder
import com.pentax.k1sync.ui.theme.DarkroomCard
import com.pentax.k1sync.ui.theme.DarkroomSurface
import com.pentax.k1sync.ui.theme.PentaxGreen
import com.pentax.k1sync.ui.theme.PentaxRed
import com.pentax.k1sync.ui.theme.TextPrimary
import com.pentax.k1sync.ui.theme.TextSecondary
import com.pentax.k1sync.viewmodel.MainViewModel

@Composable
fun RemoteScreen(viewModel: MainViewModel) {
    val cameraInfo by viewModel.cameraInfo.collectAsState()
    val params by viewModel.shootingParams.collectAsState()
    val liveFrame by viewModel.liveViewFrame.collectAsState()
    val liveActive by viewModel.liveViewActive.collectAsState()
    val liveError by viewModel.liveViewError.collectAsState()
    val manualLens by viewModel.manualLensMode.collectAsState()
    val tetherEnabled by viewModel.tetherEnabled.collectAsState()
    val gridMode by viewModel.gridMode.collectAsState()
    val shotMode by viewModel.remoteShotMode.collectAsState()
    val intervalState by viewModel.intervalState.collectAsState()
    val bulbState by viewModel.bulbState.collectAsState()

    var activeDialKey by remember { mutableStateOf<String?>(null) }
    var magnify2x by remember { mutableStateOf(false) }
    var focusIndicator by remember { mutableStateOf<Offset?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkroomBg)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Surface(color = DarkroomSurface) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("遥控与实时取景", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                ConnectionStatusBadge(cameraInfo)
            }
        }

        // Live View Monitor (3:2 full-frame aspect ratio)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 2f)
                .background(Color.Black)
                .pointerInput(manualLens) {
                    detectTapGestures(
                        onDoubleTap = { magnify2x = !magnify2x },
                        onTap = { tap ->
                            focusIndicator = tap
                            val xPct = ((tap.x / size.width) * 100).toInt().coerceIn(5, 95)
                            val yPct = ((tap.y / size.height) * 100).toInt().coerceIn(5, 95)
                            viewModel.touchFocus(xPct, yPct)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (liveFrame != null) {
                Image(
                    bitmap = liveFrame!!.asImageBitmap(),
                    contentDescription = "Live View",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = if (magnify2x) 2f else 1f,
                            scaleY = if (magnify2x) 2f else 1f
                        )
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = liveError ?: if (cameraInfo.connected) "点击下方启动 Live View 实时取景" else "请先连接 PENTAX K-1",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    if (cameraInfo.connected) {
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.startLiveView() },
                            colors = ButtonDefaults.buttonColors(containerColor = PentaxGreen)
                        ) {
                            Text("开启实时取景")
                        }
                    }
                }
            }

            // Composition Grid Overlay
            if (gridMode != GridOverlayMode.NONE) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val lineColor = Color(0x66FFFFFF)
                    when (gridMode) {
                        GridOverlayMode.THIRDS -> {
                            for (i in 1..2) {
                                val x = size.width * i / 3f
                                val y = size.height * i / 3f
                                drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.5f)
                                drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5f)
                            }
                        }
                        GridOverlayMode.CROSSHAIR -> {
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            drawLine(PentaxGreen.copy(alpha = 0.7f), Offset(cx - 28f, cy), Offset(cx + 28f, cy), strokeWidth = 2f)
                            drawLine(PentaxGreen.copy(alpha = 0.7f), Offset(cx, cy - 28f), Offset(cx, cy + 28f), strokeWidth = 2f)
                        }
                        GridOverlayMode.GOLDEN -> {
                            val r1 = 0.382f
                            val r2 = 0.618f
                            drawLine(lineColor, Offset(size.width * r1, 0f), Offset(size.width * r1, size.height), 1.5f)
                            drawLine(lineColor, Offset(size.width * r2, 0f), Offset(size.width * r2, size.height), 1.5f)
                            drawLine(lineColor, Offset(0f, size.height * r1), Offset(size.width, size.height * r1), 1.5f)
                            drawLine(lineColor, Offset(0f, size.height * r2), Offset(size.width, size.height * r2), 1.5f)
                        }
                        GridOverlayMode.NONE -> Unit
                    }
                }
            }

            // Touch Focus Reticle
            focusIndicator?.let { pt ->
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        color = PentaxGreen,
                        topLeft = Offset(pt.x - 28f, pt.y - 28f),
                        size = androidx.compose.ui.geometry.Size(56f, 56f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
                    )
                }
            }

            // Floating Live View Controls (Grid + 2x Magnifier)
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x99000000)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val all = GridOverlayMode.entries
                        val next = all[(all.indexOf(gridMode) + 1) % all.size]
                        viewModel.setGridMode(next)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.GridOn,
                        contentDescription = "网格",
                        tint = if (gridMode != GridOverlayMode.NONE) PentaxGreen else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = { magnify2x = !magnify2x },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.ZoomIn,
                        contentDescription = "放大检焦",
                        tint = if (magnify2x) PentaxGreen else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (liveActive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x99000000))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (magnify2x) "LV · 2X 检焦" else "LV · ${params.exposureMode}",
                        color = PentaxGreen,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Camera Parameter Dials Bar (Pentax LCD Style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF18181A))
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DialCell("光圈 Av", "F${params.av}", activeDialKey == "av", Modifier.weight(1f)) {
                activeDialKey = if (activeDialKey == "av") null else "av"
            }
            DialCell("快门 Tv", ShootingParams.formatTvDisplay(params.tv), activeDialKey == "tv", Modifier.weight(1f)) {
                activeDialKey = if (activeDialKey == "tv") null else "tv"
            }
            DialCell("感光 ISO", params.sv, activeDialKey == "sv", Modifier.weight(1f)) {
                activeDialKey = if (activeDialKey == "sv") null else "sv"
            }
            DialCell("补偿 Ev", params.xv, activeDialKey == "xv", Modifier.weight(1f)) {
                activeDialKey = if (activeDialKey == "xv") null else "xv"
            }
            DialCell("白平衡", ShootingParams.formatWbDisplay(params.wbMode), activeDialKey == "wbMode", Modifier.weight(1f)) {
                activeDialKey = if (activeDialKey == "wbMode") null else "wbMode"
            }
        }

        // Expandable Parameter Wheel Ribbon
        activeDialKey?.let { key ->
            val options = when (key) {
                "av" -> params.avList
                "tv" -> params.tvList
                "sv" -> params.svList
                "xv" -> params.xvList
                "wbMode" -> params.wbList
                else -> emptyList()
            }
            val currentVal = when (key) {
                "av" -> params.av
                "tv" -> params.tv
                "sv" -> params.sv
                "xv" -> params.xv
                "wbMode" -> params.wbMode
                else -> ""
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E2220))
            ) {
                items(options) { opt ->
                    val label = when (key) {
                        "av" -> "F$opt"
                        "tv" -> ShootingParams.formatTvDisplay(opt)
                        "wbMode" -> ShootingParams.formatWbDisplay(opt)
                        else -> opt
                    }
                    FilterChip(
                        selected = opt == currentVal,
                        onClick = { viewModel.updateCameraParam(key, opt) },
                        label = {
                            Text(
                                label,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PentaxGreen,
                            selectedLabelColor = Color(0xFF061A0E)
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Shooting Mode Tabs (Single / Interval / Bulb)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RemoteShotMode.entries.forEach { mode ->
                val selected = shotMode == mode
                Surface(
                    color = if (selected) PentaxGreen.copy(alpha = 0.2f) else DarkroomCard,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            1.dp,
                            if (selected) PentaxGreen else DarkroomBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { viewModel.setRemoteShotMode(mode) }
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            mode.label,
                            color = if (selected) PentaxGreen else TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Mode-Specific Shutter & Timer Panel
        when (shotMode) {
            RemoteShotMode.SINGLE -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(82.dp)
                            .clip(CircleShape)
                            .border(3.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(PentaxRed)
                            .clickable { viewModel.shootSingle() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("快门", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            RemoteShotMode.INTERVAL -> {
                Surface(
                    color = DarkroomCard,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("间隔延时 / 星轨拍摄", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("间隔时间: ${intervalState.intervalSec} 秒", color = TextPrimary, fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(3, 5, 10, 30).forEach { sec ->
                                    FilterChip(
                                        selected = intervalState.intervalSec == sec,
                                        onClick = { viewModel.updateIntervalConfig(sec, intervalState.totalShots) },
                                        label = { Text("${sec}s", fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("拍摄张数: ${intervalState.totalShots} 张", color = TextPrimary, fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(10, 30, 60, 120).forEach { cnt ->
                                    FilterChip(
                                        selected = intervalState.totalShots == cnt,
                                        onClick = { viewModel.updateIntervalConfig(intervalState.intervalSec, cnt) },
                                        label = { Text("$cnt", fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                        if (intervalState.running) {
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { intervalState.shotsTaken.toFloat() / intervalState.totalShots.coerceAtLeast(1) },
                                color = PentaxGreen,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "已拍 ${intervalState.shotsTaken}/${intervalState.totalShots} 张 · 距下一张 ${intervalState.nextShotCountdown}s",
                                color = PentaxGreen,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.toggleIntervalometer() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (intervalState.running) PentaxRed else PentaxGreen
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(if (intervalState.running) Icons.Filled.Stop else Icons.Filled.PlayArrow, null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (intervalState.running) "停止间隔拍摄" else "开始间隔延时拍摄", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            RemoteShotMode.BULB -> {
                Surface(
                    color = DarkroomCard,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("B 门长曝计时器（需将 K-1 拨盘调至 B 门）", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(15, 30, 60, 120, 300).forEach { sec ->
                                FilterChip(
                                    selected = bulbState.targetSec == sec,
                                    onClick = { viewModel.updateBulbTarget(sec) },
                                    label = { Text("${sec}s", fontSize = 11.sp) }
                                )
                            }
                        }
                        if (bulbState.running) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "正在曝光：${bulbState.elapsedSec}s / ${bulbState.targetSec}s",
                                color = PentaxRed,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.toggleBulbExposure() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (bulbState.running) PentaxRed else PentaxGreen
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (bulbState.running) "立即结束 B 门曝光" else "启动 ${bulbState.targetSec}s 长曝光", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Pentax Manual Lens & Tether Switches
        Surface(
            color = DarkroomCard,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("手动头安全快门 (跳过 AF)", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("适配 K/M/A/M42 老镜头，不等待对焦直接释放快门", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = manualLens,
                        onCheckedChange = { viewModel.setManualLensMode(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PentaxGreen)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("拍完即传 (Tether 模式)", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("监听相机推送，按快门后自动回传预览图至手机相册", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = tetherEnabled,
                        onCheckedChange = { viewModel.toggleTetherMode(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PentaxGreen)
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DialCell(
    label: String,
    value: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) PentaxGreen.copy(alpha = 0.22f) else DarkroomCard)
            .border(1.dp, if (selected) PentaxGreen else DarkroomBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = TextSecondary, fontSize = 10.sp)
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            color = if (selected) PentaxGreen else TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1
        )
    }
}

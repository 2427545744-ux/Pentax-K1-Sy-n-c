package com.pentax.k1sync.ui.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pentax.k1sync.model.DownloadQuality
import com.pentax.k1sync.model.TransferStatus
import com.pentax.k1sync.model.TransferTask
import com.pentax.k1sync.ui.components.ConnectionStatusBadge
import com.pentax.k1sync.ui.components.SectionHeader
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
fun SyncScreen(viewModel: MainViewModel) {
    val cameraInfo by viewModel.cameraInfo.collectAsState()
    val wifiStatus by viewModel.wifiStatus.collectAsState()
    val isConnecting by viewModel.isConnecting.collectAsState()
    val isMock by viewModel.isMockMode.collectAsState()
    val defaultQuality by viewModel.defaultQuality.collectAsState()
    val autoWatermark by viewModel.autoWatermarkOnTransfer.collectAsState()
    val tasks by viewModel.transferTasks.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkroomBg)
            .verticalScroll(rememberScrollState())
    ) {
        Surface(color = DarkroomSurface) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("连接与后台传输", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                ConnectionStatusBadge(cameraInfo)
            }
        }

        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Camera Connection Card
            Surface(color = DarkroomCard, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (cameraInfo.connected) cameraInfo.model else "PENTAX K-1（一代）",
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = wifiStatus,
                                color = if (cameraInfo.connected) PentaxGreen else TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        if (isConnecting) {
                            CircularProgressIndicator(color = PentaxGreen, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        }
                    }

                    if (cameraInfo.connected) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatusMiniChip(
                                icon = Icons.Filled.BatteryFull,
                                label = "电池 ${cameraInfo.batteryPercent}%",
                                modifier = Modifier.weight(1f)
                            )
                            cameraInfo.storages.forEach { st ->
                                StatusMiniChip(
                                    icon = Icons.Filled.SdStorage,
                                    label = "${st.slot.displayName}: ${if (st.available) "余 ${st.remainShots}张" else "未插卡"}",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.smartConnectPentaxWifi() },
                            colors = ButtonDefaults.buttonColors(containerColor = PentaxGreen),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("搜索 K-1 热点", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = { viewModel.bindCurrentWifiAndConnect() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("绑定当前 Wi-Fi", color = TextPrimary, fontSize = 13.sp)
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.syncPhoneTimeToCamera() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.AccessTime, contentDescription = null, modifier = Modifier.size(15.dp), tint = PentaxGreen)
                            Spacer(Modifier.width(4.dp))
                            Text("同步手机时间到机身", color = TextPrimary, fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { viewModel.clearThumbnailCache() },
                            modifier = Modifier.weight(0.85f)
                        ) {
                            Text("清理缓存 (${"%.1f".format(viewModel.cacheSizeMb())}MB)", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Mock Camera Mode Toggle
            Surface(color = DarkroomCard, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("虚拟 K-1 演示模式 (Mock)", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("无需开机即可测试图库、直方图、遥控与宾得水印导出；连实机请关闭", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = isMock,
                        onCheckedChange = { viewModel.toggleMockMode(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFFB74D))
                    )
                }
            }

            // Transfer Quality Strategy
            SectionHeader("默认传输策略", "后台保活不断传")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DownloadQuality.entries.forEach { q ->
                    val selected = defaultQuality == q
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
                            .clickable { viewModel.setDefaultQuality(q) }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(q.label, color = if (selected) PentaxGreen else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(q.desc, color = TextSecondary, fontSize = 10.sp, lineHeight = 13.sp)
                        }
                    }
                }
            }

            Surface(color = DarkroomCard, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("传图时自动额外生成宾得水印副本", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("同时保留干净原图 + 存入 PentaxK1_Watermark", color = TextSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = autoWatermark,
                        onCheckedChange = { viewModel.setAutoWatermarkOnTransfer(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PentaxGreen)
                    )
                }
            }

            // One-tap Sync New Photos
            Button(
                onClick = { viewModel.enqueueOnlyUndownloaded() },
                enabled = cameraInfo.connected,
                colors = ButtonDefaults.buttonColors(containerColor = PentaxGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("一键同步当前卡内所有未下载新照片", fontWeight = FontWeight.Bold)
            }

            // Active Transfer Queue
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("传输队列 (${tasks.size})", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                if (tasks.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "清理已完成",
                            color = PentaxGreen,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable { viewModel.clearCompletedTasks() }
                        )
                        Text(
                            text = "全部取消",
                            color = PentaxRed,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable { viewModel.cancelAllPendingTasks() }
                        )
                    }
                }
            }

            if (tasks.isEmpty()) {
                Surface(color = DarkroomCard, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "暂无传输任务。在图库长按多选照片或点击“一键同步”即可开始后台传图。",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                tasks.forEach { task ->
                    TransferTaskRow(task)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatusMiniChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(DarkroomBg, RoundedCornerShape(8.dp))
            .border(1.dp, DarkroomBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = PentaxGreen, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, color = TextPrimary, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable
private fun TransferTaskRow(task: TransferTask) {
    Surface(color = DarkroomCard, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(task.photo.file, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                        text = "${task.quality.label} · ${task.savedPath ?: task.errorMessage ?: task.status.name}",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                when (task.status) {
                    TransferStatus.COMPLETED -> Icon(Icons.Filled.CheckCircle, null, tint = PentaxGreen, modifier = Modifier.size(20.dp))
                    TransferStatus.FAILED -> Icon(Icons.Filled.Error, null, tint = PentaxRed, modifier = Modifier.size(20.dp))
                    TransferStatus.CANCELLED -> Icon(Icons.Filled.Delete, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                    TransferStatus.DOWNLOADING -> Text(
                        "${(task.progress * 100).toInt()}%",
                        color = PentaxGreen,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    TransferStatus.QUEUED -> Text("排队中", color = TextSecondary, fontSize = 11.sp)
                }
            }
            if (task.status == TransferStatus.DOWNLOADING) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { task.progress },
                    color = PentaxGreen,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

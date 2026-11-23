package com.pentax.k1sync.ui.watermark

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pentax.k1sync.ui.theme.DarkroomBg
import com.pentax.k1sync.ui.theme.DarkroomBorder
import com.pentax.k1sync.ui.theme.DarkroomCard
import com.pentax.k1sync.ui.theme.DarkroomSurface
import com.pentax.k1sync.ui.theme.PentaxGreen
import com.pentax.k1sync.ui.theme.TextPrimary
import com.pentax.k1sync.ui.theme.TextSecondary
import com.pentax.k1sync.viewmodel.MainViewModel
import com.pentax.k1sync.watermark.PentaxAccentColor
import com.pentax.k1sync.watermark.WatermarkStyle

@Composable
fun WatermarkScreen(viewModel: MainViewModel) {
    val state by viewModel.watermarkState.collectAsState()
    val cfg = state.config

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.loadLocalPhotoForWatermark(uri)
        }
    }

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
                Column {
                    Text("宾得 K-1 水印工坊", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("自动读取 EXIF · 支持手动老头参数微调", color = TextSecondary, fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                ) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp), tint = PentaxGreen)
                    Spacer(Modifier.width(4.dp))
                    Text("选手机图", color = TextPrimary, fontSize = 12.sp)
                }
            }
        }

        // Live Watermark Preview Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 240.dp, max = 330.dp)
                .background(Color(0xFF0A0A0C))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (state.previewBitmap != null) {
                Image(
                    bitmap = state.previewBitmap!!.asImageBitmap(),
                    contentDescription = "Watermark Preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (state.isRendering) {
                CircularProgressIndicator(color = PentaxGreen, modifier = Modifier.size(30.dp))
            }
        }

        // 3 Reference Watermark Templates Selector
        Column(modifier = Modifier.padding(14.dp)) {
            Text("选择宾得水印模板（共 3 款）", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WatermarkStyle.entries.forEach { style ->
                    val selected = cfg.style == style
                    Surface(
                        color = if (selected) PentaxGreen.copy(alpha = 0.18f) else DarkroomCard,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) PentaxGreen else DarkroomBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                viewModel.updateWatermarkConfig { it.copy(style = style) }
                            }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = style.title,
                                color = if (selected) PentaxGreen else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = style.subtitle,
                                color = TextSecondary,
                                fontSize = 10.sp,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Pentax Accent Color Picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("品牌徽标点缀色", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PentaxAccentColor.entries.forEach { accent ->
                        val chosen = cfg.accentColor == accent
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (chosen) DarkroomCard else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (chosen) PentaxGreen else DarkroomBorder,
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable {
                                    viewModel.updateWatermarkConfig { it.copy(accentColor = accent) }
                                }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(accent.colorInt))
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(accent.label, color = TextPrimary, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Editable Camera & EXIF Fields (Crucial for Pentax manual lenses!)
            Surface(color = DarkroomCard, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("参数信息编辑（老镜头可手动补全）", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CompactField("机身型号", cfg.cameraTitle, Modifier.weight(1f)) { v ->
                            viewModel.updateWatermarkConfig { it.copy(cameraTitle = v) }
                        }
                        CompactField("镜头名称 (可选)", cfg.lensTitle, Modifier.weight(1.3f)) { v ->
                            viewModel.updateWatermarkConfig { it.copy(lensTitle = v) }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CompactField("焦距", cfg.focalLength, Modifier.weight(1f)) { v ->
                            viewModel.updateWatermarkConfig { it.copy(focalLength = v) }
                        }
                        CompactField("光圈", cfg.aperture, Modifier.weight(1f)) { v ->
                            viewModel.updateWatermarkConfig { it.copy(aperture = v) }
                        }
                        CompactField("快门", cfg.shutterSpeed, Modifier.weight(1f)) { v ->
                            viewModel.updateWatermarkConfig { it.copy(shutterSpeed = v) }
                        }
                        CompactField("ISO", cfg.iso, Modifier.weight(1f)) { v ->
                            viewModel.updateWatermarkConfig { it.copy(iso = v) }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CompactField("拍摄时间", cfg.dateTime, Modifier.weight(1.2f)) { v ->
                            viewModel.updateWatermarkConfig { it.copy(dateTime = v) }
                        }
                        CompactField("摄影师署名", cfg.signature, Modifier.weight(1f)) { v ->
                            viewModel.updateWatermarkConfig { it.copy(signature = v) }
                        }
                    }

                    // Toggles
                    WatermarkToggleRow("显示拍摄日期时间", cfg.showDateTime) { checked ->
                        viewModel.updateWatermarkConfig { it.copy(showDateTime = checked) }
                    }
                    WatermarkToggleRow("显示镜头型号副标题", cfg.showLensName) { checked ->
                        viewModel.updateWatermarkConfig { it.copy(showLensName = checked) }
                    }
                    if (cfg.style == WatermarkStyle.MINIMAL_WHITE) {
                        WatermarkToggleRow("在画面底部叠加半透明署名角标", cfg.showOverlaySignature) { checked ->
                            viewModel.updateWatermarkConfig { it.copy(showOverlaySignature = checked) }
                        }
                    }
                    WatermarkToggleRow("原分辨率高清导出 (JPG 97%)", cfg.highQualityExport) { checked ->
                        viewModel.updateWatermarkConfig { it.copy(highQualityExport = checked) }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Export Button
            Button(
                onClick = { viewModel.exportWatermarkedPhoto() },
                enabled = !state.isSaving && state.sourceBitmap != null,
                colors = ButtonDefaults.buttonColors(containerColor = PentaxGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(color = Color(0xFF061A0E), modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("保存宾得水印大片到系统相册", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CompactField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 10.sp) },
        singleLine = true,
        modifier = modifier,
        textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 12.sp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PentaxGreen,
            unfocusedBorderColor = DarkroomBorder,
            focusedLabelColor = PentaxGreen,
            unfocusedLabelColor = TextSecondary
        )
    )
}

@Composable
private fun WatermarkToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextPrimary, fontSize = 13.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PentaxGreen)
        )
    }
}

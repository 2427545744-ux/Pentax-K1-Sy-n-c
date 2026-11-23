package com.pentax.k1sync.ui.gallery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pentax.k1sync.model.DownloadQuality
import com.pentax.k1sync.ui.components.RgbHistogramView
import com.pentax.k1sync.ui.theme.DarkroomCard
import com.pentax.k1sync.ui.theme.PentaxGreen
import com.pentax.k1sync.ui.theme.TextPrimary
import com.pentax.k1sync.ui.theme.TextSecondary
import com.pentax.k1sync.viewmodel.MainViewModel

@Composable
fun PhotoDetailDialog(viewModel: MainViewModel) {
    val photo by viewModel.detailPhoto.collectAsState()
    val bitmap by viewModel.detailBitmap.collectAsState()
    val exif by viewModel.detailExif.collectAsState()
    val histogram by viewModel.detailHistogram.collectAsState()
    val isLoading by viewModel.isLoadingDetail.collectAsState()

    val currentPhoto = photo ?: return

    var showExifHud by remember { mutableStateOf(true) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = { viewModel.closePhotoDetail() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Zoomable Photo Viewer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            scale = newScale
                            offset = if (newScale > 1f) offset + pan else Offset.Zero
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = currentPhoto.file,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            )
                    )
                }
                if (isLoading) {
                    CircularProgressIndicator(color = PentaxGreen)
                }
            }

            // Top Toolbar
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(Color(0xAA000000))
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.closePhotoDetail() }) {
                        Icon(Icons.Filled.Close, contentDescription = "关闭", tint = Color.White)
                    }
                    Column {
                        Text(currentPhoto.file, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            "${currentPhoto.slot.displayName} / ${currentPhoto.dir}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
                IconButton(onClick = { showExifHud = !showExifHud }) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = "EXIF",
                        tint = if (showExifHud) PentaxGreen else Color.White
                    )
                }
            }

            // Bottom EXIF + Histogram + Action Bar
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xDD101012))
                    .padding(14.dp)
            ) {
                if (showExifHud) {
                    histogram?.let {
                        RgbHistogramView(histogram = it)
                        Spacer(Modifier.height(8.dp))
                    }
                    exif?.let { info ->
                        Surface(
                            color = DarkroomCard,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        info.cameraModel.ifBlank { "PENTAX K-1" },
                                        color = PentaxGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(info.dateTime, color = TextSecondary, fontSize = 11.sp)
                                }
                                if (info.lensModel.isNotBlank()) {
                                    Text(info.lensModel, color = TextPrimary, fontSize = 12.sp)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = info.formatParamsSingleLine().ifBlank { "等待相机返回 EXIF 参数..." },
                                    color = TextPrimary,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.enqueueDownload(listOf(currentPhoto), DownloadQuality.VIEW)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("预览下载", color = TextPrimary, fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            val q = if (currentPhoto.isRaw) DownloadQuality.RAW else DownloadQuality.FULL_JPG
                            viewModel.enqueueDownload(listOf(currentPhoto), q)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkroomCard),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextPrimary)
                        Spacer(Modifier.width(4.dp))
                        Text(if (currentPhoto.isRaw) "下 RAW" else "下原图", color = TextPrimary, fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            viewModel.sendPhotoToWatermarkStudio(currentPhoto, bitmap, exif)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PentaxGreen),
                        modifier = Modifier.weight(1.15f)
                    ) {
                        Icon(Icons.Filled.BrandingWatermark, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("宾得水印", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

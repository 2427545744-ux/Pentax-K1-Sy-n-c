package com.pentax.k1sync.ui.gallery

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pentax.k1sync.model.CameraPhoto
import com.pentax.k1sync.model.DownloadQuality
import com.pentax.k1sync.model.PhotoFormatFilter
import com.pentax.k1sync.model.StorageSlot
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(viewModel: MainViewModel) {
    val cameraInfo by viewModel.cameraInfo.collectAsState()
    val activeSlot by viewModel.activeSlot.collectAsState()
    val formatFilter by viewModel.formatFilter.collectAsState()
    val photos by viewModel.photos.collectAsState()
    val isLoading by viewModel.isLoadingPhotos.collectAsState()
    val selectedIds by viewModel.selectedPhotoIds.collectAsState()
    val thumbnails by viewModel.thumbnails.collectAsState()

    val filteredPhotos = remember(photos, formatFilter) {
        when (formatFilter) {
            PhotoFormatFilter.ALL -> photos
            PhotoFormatFilter.JPG -> photos.filter { !it.isRaw }
            PhotoFormatFilter.RAW -> photos.filter { it.isRaw }
        }
    }

    val groupedPhotos = remember(filteredPhotos) {
        filteredPhotos.groupBy { it.dateGroup }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkroomBg)
    ) {
        // Top Header Bar
        Surface(color = DarkroomSurface, tonalElevation = 2.dp) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "PENTAX",
                            color = PentaxGreen,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "K-1",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ConnectionStatusBadge(cameraInfo)
                        IconButton(onClick = { viewModel.refreshPhotos() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "刷新", tint = TextPrimary)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // SD1/SD2 switcher + Format filter chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkroomBg)
                            .border(1.dp, DarkroomBorder, RoundedCornerShape(10.dp))
                            .padding(2.dp)
                    ) {
                        StorageSlot.entries.forEach { slot ->
                            val selected = activeSlot == slot
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) PentaxGreen else Color.Transparent)
                                    .combinedClickable(onClick = { viewModel.switchStorageSlot(slot) })
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = slot.displayName,
                                    color = if (selected) Color(0xFF061A0E) else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PhotoFormatFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = formatFilter == filter,
                                onClick = { viewModel.setFormatFilter(filter) },
                                label = { Text(filter.label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PentaxGreen.copy(alpha = 0.22f),
                                    selectedLabelColor = PentaxGreen
                                )
                            )
                        }
                    }
                }
            }
        }

        // Multi-select action strip
        if (selectedIds.isNotEmpty()) {
            Surface(color = Color(0xFF192920), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.clearSelection() }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "取消选择", tint = TextPrimary)
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("已选 ${selectedIds.size} 张", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = { viewModel.selectAllFiltered(filteredPhotos) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Filled.SelectAll, contentDescription = "全选", tint = PentaxGreen)
                        }
                        Button(
                            onClick = {
                                val chosen = filteredPhotos.filter { selectedIds.contains(it.uniqueId) }
                                viewModel.enqueueDownload(chosen, DownloadQuality.VIEW)
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkroomCard)
                        ) {
                            Text("预览图", color = TextPrimary, fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                val chosen = filteredPhotos.filter { selectedIds.contains(it.uniqueId) }
                                viewModel.enqueueDownload(chosen, DownloadQuality.FULL_JPG)
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PentaxGreen)
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("传原图", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Main Gallery Content
        if (!cameraInfo.connected) {
            DisconnectedGalleryPlaceholder(
                onConnectWifi = { viewModel.smartConnectPentaxWifi() },
                onEnableMock = { viewModel.toggleMockMode(true) }
            )
        } else if (isLoading && filteredPhotos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PentaxGreen)
                    Spacer(Modifier.height(12.dp))
                    Text("正在读取 ${activeSlot.displayName} 索引...", color = TextSecondary, fontSize = 13.sp)
                }
            }
        } else if (filteredPhotos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("当前卡槽或筛选条件下没有照片", color = TextSecondary, fontSize = 14.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                groupedPhotos.forEach { (dateGroup, itemsInGroup) ->
                    item(span = { GridItemSpan(3) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 4.dp, start = 4.dp, end = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = dateGroup,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${itemsInGroup.size} 张",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    items(itemsInGroup, key = { it.uniqueId }) { photo ->
                        PhotoGridTile(
                            photo = photo,
                            bitmap = thumbnails[photo.uniqueId],
                            isSelected = selectedIds.contains(photo.uniqueId),
                            inSelectionMode = selectedIds.isNotEmpty(),
                            onRequestThumb = { viewModel.requestThumbnail(photo) },
                            onClick = {
                                if (selectedIds.isNotEmpty()) {
                                    viewModel.togglePhotoSelection(photo)
                                } else {
                                    viewModel.openPhotoDetail(photo)
                                }
                            },
                            onLongClick = { viewModel.togglePhotoSelection(photo) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoGridTile(
    photo: CameraPhoto,
    bitmap: android.graphics.Bitmap?,
    isSelected: Boolean,
    inSelectionMode: Boolean,
    onRequestThumb: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    LaunchedEffect(photo.uniqueId) {
        if (bitmap == null) {
            onRequestThumb()
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(DarkroomCard)
            .border(
                width = if (isSelected) 2.dp else 0.5.dp,
                color = if (isSelected) PentaxGreen else DarkroomBorder,
                shape = RoundedCornerShape(6.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = photo.file,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.PhotoLibrary,
                    contentDescription = null,
                    tint = TextSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // Format badge (PEF / DNG / JPG)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(4.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (photo.isRaw) PentaxRed.copy(alpha = 0.88f) else Color(0xAA101010))
                .padding(horizontal = 5.dp, vertical = 1.dp)
        ) {
            Text(
                text = photo.extension,
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // Downloaded badge
        if (photo.isDownloaded && !inSelectionMode) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "已下载",
                tint = PentaxGreen,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(16.dp)
                    .background(Color(0xBB000000), CircleShape)
            )
        }

        // Selection indicator
        if (inSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) PentaxGreen else Color(0x99000000))
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF061A0E),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DisconnectedGalleryPlaceholder(
    onConnectWifi: () -> Unit,
    onEnableMock: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Wifi,
                contentDescription = null,
                tint = PentaxGreen,
                modifier = Modifier.size(54.dp)
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = "请先连接 PENTAX K-1 无线热点",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "在 K-1 机身开启 Wi-Fi 后点击下方按钮，App 将自动锁定相机无线通道，防止安卓切回 5G 导致断连。",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(22.dp))
            Button(
                onClick = onConnectWifi,
                colors = ButtonDefaults.buttonColors(containerColor = PentaxGreen),
                modifier = Modifier.fillMaxWidth(0.82f)
            ) {
                Text("智能直连 PENTAX K-1 Wi-Fi", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onEnableMock,
                modifier = Modifier.fillMaxWidth(0.82f)
            ) {
                Text("开启虚拟 K-1 演示模式（离线体验）", color = TextPrimary)
            }
        }
    }
}

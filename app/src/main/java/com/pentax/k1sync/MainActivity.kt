package com.pentax.k1sync

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pentax.k1sync.ui.gallery.GalleryScreen
import com.pentax.k1sync.ui.gallery.PhotoDetailDialog
import com.pentax.k1sync.ui.remote.RemoteScreen
import com.pentax.k1sync.ui.sync.SyncScreen
import com.pentax.k1sync.ui.theme.DarkroomBg
import com.pentax.k1sync.ui.theme.DarkroomSurface
import com.pentax.k1sync.ui.theme.PentaxGreen
import com.pentax.k1sync.ui.theme.PentaxK1Theme
import com.pentax.k1sync.ui.theme.TextSecondary
import com.pentax.k1sync.ui.watermark.WatermarkScreen
import com.pentax.k1sync.viewmodel.AppTab
import com.pentax.k1sync.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Permissions handled gracefully at feature use time */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestRuntimePermissions()

        setContent {
            PentaxK1Theme {
                K1SyncRootScreen()
            }
        }
    }

    private fun requestRuntimePermissions() {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms += Manifest.permission.POST_NOTIFICATIONS
            perms += Manifest.permission.READ_MEDIA_IMAGES
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            perms += Manifest.permission.WRITE_EXTERNAL_STORAGE
            perms += Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (perms.isNotEmpty()) {
            permissionLauncher.launch(perms.toTypedArray())
        }
    }
}

@Composable
fun K1SyncRootScreen(viewModel: MainViewModel = viewModel()) {
    val activeTab by viewModel.activeTab.collectAsState()
    val toastMsg by viewModel.toastMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMsg) {
        if (!toastMsg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(toastMsg!!)
            viewModel.consumeToast()
        }
    }

    Scaffold(
        containerColor = DarkroomBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = DarkroomSurface) {
                val items = listOf(
                    Triple(AppTab.GALLERY, "图库", Icons.Filled.PhotoLibrary),
                    Triple(AppTab.REMOTE, "遥控", Icons.Filled.CameraAlt),
                    Triple(AppTab.WATERMARK, "水印", Icons.Filled.BrandingWatermark),
                    Triple(AppTab.SYNC, "连接", Icons.Filled.Sync)
                )
                items.forEach { (tab, label, icon) ->
                    NavigationBarItem(
                        selected = activeTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PentaxGreen,
                            selectedTextColor = PentaxGreen,
                            indicatorColor = PentaxGreen.copy(alpha = 0.16f),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                AppTab.GALLERY -> GalleryScreen(viewModel)
                AppTab.REMOTE -> RemoteScreen(viewModel)
                AppTab.WATERMARK -> WatermarkScreen(viewModel)
                AppTab.SYNC -> SyncScreen(viewModel)
            }
        }
    }

    // Full-screen zoomable photo detail + EXIF + RGB Histogram overlay
    PhotoDetailDialog(viewModel = viewModel)
}

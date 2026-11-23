package com.pentax.k1sync.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pentax.k1sync.K1SyncApp
import com.pentax.k1sync.model.CameraInfo
import com.pentax.k1sync.model.CameraPhoto
import com.pentax.k1sync.model.DownloadQuality
import com.pentax.k1sync.model.GridOverlayMode
import com.pentax.k1sync.model.HistogramData
import com.pentax.k1sync.model.PhotoExif
import com.pentax.k1sync.model.PhotoFormatFilter
import com.pentax.k1sync.model.RemoteShotMode
import com.pentax.k1sync.model.ShootingParams
import com.pentax.k1sync.model.StorageSlot
import com.pentax.k1sync.model.TransferStatus
import com.pentax.k1sync.model.TransferTask
import com.pentax.k1sync.service.TransferForegroundService
import com.pentax.k1sync.util.HistogramCalculator
import com.pentax.k1sync.util.MediaStoreSaver
import com.pentax.k1sync.watermark.PentaxWatermarkRenderer
import com.pentax.k1sync.watermark.WatermarkConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.util.UUID

enum class AppTab(val label: String) {
    GALLERY("图库"),
    REMOTE("遥控"),
    WATERMARK("水印"),
    SYNC("连接")
}

data class IntervalState(
    val running: Boolean = false,
    val intervalSec: Int = 5,
    val totalShots: Int = 20,
    val shotsTaken: Int = 0,
    val nextShotCountdown: Int = 0
)

data class BulbState(
    val running: Boolean = false,
    val targetSec: Int = 30,
    val elapsedSec: Int = 0
)

data class WatermarkEditorState(
    val sourceBitmap: Bitmap? = null,
    val previewBitmap: Bitmap? = null,
    val sourceFileName: String = "PENTAX_K1_Sample.jpg",
    val config: WatermarkConfig = WatermarkConfig(),
    val isRendering: Boolean = false,
    val isSaving: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as K1SyncApp).repository

    private val _activeTab = MutableStateFlow(AppTab.GALLERY)
    val activeTab: StateFlow<AppTab> = _activeTab.asStateFlow()

    private val _cameraInfo = MutableStateFlow(CameraInfo())
    val cameraInfo: StateFlow<CameraInfo> = _cameraInfo.asStateFlow()

    private val _shootingParams = MutableStateFlow(ShootingParams())
    val shootingParams: StateFlow<ShootingParams> = _shootingParams.asStateFlow()

    private val _isMockMode = MutableStateFlow(false)
    val isMockMode: StateFlow<Boolean> = _isMockMode.asStateFlow()

    val wifiStatus: StateFlow<String> = repo.wifiBinder.statusMessage

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // --- Gallery State ---
    private val _activeSlot = MutableStateFlow(StorageSlot.SD1)
    val activeSlot: StateFlow<StorageSlot> = _activeSlot.asStateFlow()

    private val _formatFilter = MutableStateFlow(PhotoFormatFilter.ALL)
    val formatFilter: StateFlow<PhotoFormatFilter> = _formatFilter.asStateFlow()

    private val _photos = MutableStateFlow<List<CameraPhoto>>(emptyList())
    val photos: StateFlow<List<CameraPhoto>> = _photos.asStateFlow()

    private val _isLoadingPhotos = MutableStateFlow(false)
    val isLoadingPhotos: StateFlow<Boolean> = _isLoadingPhotos.asStateFlow()

    private val _selectedPhotoIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedPhotoIds: StateFlow<Set<String>> = _selectedPhotoIds.asStateFlow()

    private val _thumbnails = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val thumbnails: StateFlow<Map<String, Bitmap>> = _thumbnails.asStateFlow()

    // --- Photo Detail State ---
    private val _detailPhoto = MutableStateFlow<CameraPhoto?>(null)
    val detailPhoto: StateFlow<CameraPhoto?> = _detailPhoto.asStateFlow()

    private val _detailBitmap = MutableStateFlow<Bitmap?>(null)
    val detailBitmap: StateFlow<Bitmap?> = _detailBitmap.asStateFlow()

    private val _detailExif = MutableStateFlow<PhotoExif?>(null)
    val detailExif: StateFlow<PhotoExif?> = _detailExif.asStateFlow()

    private val _detailHistogram = MutableStateFlow<HistogramData?>(null)
    val detailHistogram: StateFlow<HistogramData?> = _detailHistogram.asStateFlow()

    private val _isLoadingDetail = MutableStateFlow(false)
    val isLoadingDetail: StateFlow<Boolean> = _isLoadingDetail.asStateFlow()

    // --- Transfer Queue State ---
    private val _defaultQuality = MutableStateFlow(DownloadQuality.VIEW)
    val defaultQuality: StateFlow<DownloadQuality> = _defaultQuality.asStateFlow()

    private val _autoWatermarkOnTransfer = MutableStateFlow(false)
    val autoWatermarkOnTransfer: StateFlow<Boolean> = _autoWatermarkOnTransfer.asStateFlow()

    private val _transferTasks = MutableStateFlow<List<TransferTask>>(emptyList())
    val transferTasks: StateFlow<List<TransferTask>> = _transferTasks.asStateFlow()

    private var transferWorkerJob: Job? = null

    // --- Remote Control State ---
    private val _liveViewFrame = MutableStateFlow<Bitmap?>(null)
    val liveViewFrame: StateFlow<Bitmap?> = _liveViewFrame.asStateFlow()

    private val _liveViewActive = MutableStateFlow(false)
    val liveViewActive: StateFlow<Boolean> = _liveViewActive.asStateFlow()

    private val _liveViewError = MutableStateFlow<String?>(null)
    val liveViewError: StateFlow<String?> = _liveViewError.asStateFlow()

    private val _manualLensMode = MutableStateFlow(false)
    val manualLensMode: StateFlow<Boolean> = _manualLensMode.asStateFlow()

    private val _tetherEnabled = MutableStateFlow(false)
    val tetherEnabled: StateFlow<Boolean> = _tetherEnabled.asStateFlow()

    private val _gridMode = MutableStateFlow(GridOverlayMode.THIRDS)
    val gridMode: StateFlow<GridOverlayMode> = _gridMode.asStateFlow()

    private val _remoteShotMode = MutableStateFlow(RemoteShotMode.SINGLE)
    val remoteShotMode: StateFlow<RemoteShotMode> = _remoteShotMode.asStateFlow()

    private val _intervalState = MutableStateFlow(IntervalState())
    val intervalState: StateFlow<IntervalState> = _intervalState.asStateFlow()

    private val _bulbState = MutableStateFlow(BulbState())
    val bulbState: StateFlow<BulbState> = _bulbState.asStateFlow()

    private var liveViewJob: Job? = null
    private var intervalJob: Job? = null
    private var bulbJob: Job? = null

    // --- Watermark Studio State ---
    private val _watermarkState = MutableStateFlow(WatermarkEditorState())
    val watermarkState: StateFlow<WatermarkEditorState> = _watermarkState.asStateFlow()
    private var watermarkRenderJob: Job? = null

    init {
        // Preload a sample image into Watermark Studio so the user can preview styles immediately
        viewModelScope.launch(Dispatchers.Default) {
            val samplePhoto = repo.mockRepo.listPhotos(StorageSlot.SD1).first()
            val bytes = repo.mockRepo.renderMockPhotoBytes(samplePhoto, 1440, 960)
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bmp != null) {
                val cfg = WatermarkConfig.fromExif(samplePhoto.exif ?: PhotoExif())
                _watermarkState.update {
                    it.copy(
                        sourceBitmap = bmp,
                        sourceFileName = samplePhoto.file,
                        config = cfg
                    )
                }
                triggerWatermarkPreview()
            }
        }
    }

    fun selectTab(tab: AppTab) {
        _activeTab.value = tab
        if (tab == AppTab.REMOTE && _cameraInfo.value.connected && !_liveViewActive.value) {
            startLiveView()
        } else if (tab != AppTab.REMOTE && _liveViewActive.value) {
            stopLiveView()
        }
    }

    fun consumeToast() {
        _toastMessage.value = null
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    // ---------------------------------------------------------
    // Connection & Wi-Fi Engine
    // ---------------------------------------------------------
    fun toggleMockMode(enabled: Boolean) {
        stopLiveView()
        stopTether()
        repo.isMockMode = enabled
        _isMockMode.value = enabled
        if (enabled) {
            connectCamera()
        } else {
            _cameraInfo.value = CameraInfo(connected = false)
            _photos.value = emptyList()
            _selectedPhotoIds.value = emptySet()
            showToast("已切换为真实 K-1 通信模式 (192.168.0.1)")
        }
    }

    fun smartConnectPentaxWifi() {
        if (_isMockMode.value) {
            connectCamera()
            return
        }
        _isConnecting.value = true
        repo.wifiBinder.requestPentaxHotspot { bound ->
            viewModelScope.launch {
                if (bound) {
                    delay(600)
                }
                connectCamera()
            }
        }
    }

    fun bindCurrentWifiAndConnect() {
        if (_isMockMode.value) {
            connectCamera()
            return
        }
        _isConnecting.value = true
        repo.wifiBinder.bindToCurrentWifi {
            viewModelScope.launch {
                delay(400)
                connectCamera()
            }
        }
    }

    fun connectCamera() {
        viewModelScope.launch {
            _isConnecting.value = true
            repo.connectAndFetchStatus()
                .onSuccess { (info, params) ->
                    _cameraInfo.value = info
                    _shootingParams.value = params
                    showToast(if (info.isMock) "已连接虚拟 PENTAX K-1（演示模式）" else "已连接 ${info.model}")
                    refreshPhotos()
                }
                .onFailure { err ->
                    _cameraInfo.value = CameraInfo(connected = false)
                    showToast("无法连接 K-1 (192.168.0.1): ${err.localizedMessage ?: "请确认手机已连入 PENTAX_xxxx Wi-Fi"}")
                }
            _isConnecting.value = false
        }
    }

    fun syncPhoneTimeToCamera() {
        if (!_cameraInfo.value.connected) {
            showToast("请先连接 PENTAX K-1")
            return
        }
        viewModelScope.launch {
            repo.syncPhoneTime()
                .onSuccess { showToast("已将手机时间同步至 K-1：$it") }
                .onFailure { showToast("时间同步失败: ${it.localizedMessage}") }
        }
    }

    fun clearThumbnailCache() {
        repo.thumbCache.clearAll()
        _thumbnails.value = emptyMap()
        showToast("已清理本地缩略图缓存")
    }

    fun cacheSizeMb(): Float = repo.thumbCache.cacheSizeMb()

    // ---------------------------------------------------------
    // Gallery & Photo Detail
    // ---------------------------------------------------------
    fun switchStorageSlot(slot: StorageSlot) {
        if (_activeSlot.value == slot) return
        _activeSlot.value = slot
        _selectedPhotoIds.value = emptySet()
        refreshPhotos()
    }

    fun setFormatFilter(filter: PhotoFormatFilter) {
        _formatFilter.value = filter
    }

    fun refreshPhotos() {
        if (!_cameraInfo.value.connected) return
        viewModelScope.launch {
            _isLoadingPhotos.value = true
            repo.listPhotos(_activeSlot.value)
                .onSuccess { list ->
                    _photos.value = list
                }
                .onFailure { err ->
                    showToast("读取 ${_activeSlot.value.displayName} 失败: ${err.localizedMessage}")
                }
            _isLoadingPhotos.value = false
        }
    }

    fun requestThumbnail(photo: CameraPhoto) {
        if (_thumbnails.value.containsKey(photo.uniqueId)) return
        viewModelScope.launch {
            val bmp = repo.loadThumbnail(photo)
            if (bmp != null) {
                _thumbnails.update { it + (photo.uniqueId to bmp) }
            }
        }
    }

    fun togglePhotoSelection(photo: CameraPhoto) {
        _selectedPhotoIds.update { current ->
            if (current.contains(photo.uniqueId)) current - photo.uniqueId else current + photo.uniqueId
        }
    }

    fun selectAllFiltered(filtered: List<CameraPhoto>) {
        _selectedPhotoIds.value = filtered.map { it.uniqueId }.toSet()
    }

    fun clearSelection() {
        _selectedPhotoIds.value = emptySet()
    }

    fun openPhotoDetail(photo: CameraPhoto) {
        _detailPhoto.value = photo
        _detailBitmap.value = _thumbnails.value[photo.uniqueId]
        _detailExif.value = photo.exif
        _detailHistogram.value = null
        _isLoadingDetail.value = true

        viewModelScope.launch {
            launch {
                val exif = repo.fetchPhotoExif(photo)
                _detailExif.value = exif
            }
            repo.loadPreviewBitmap(photo)
                .onSuccess { bmp ->
                    _detailBitmap.value = bmp
                    _detailHistogram.value = HistogramCalculator.compute(bmp)
                }
                .onFailure { err ->
                    showToast("加载大图失败: ${err.localizedMessage}")
                }
            _isLoadingDetail.value = false
        }
    }

    fun closePhotoDetail() {
        _detailPhoto.value = null
        _detailBitmap.value = null
        _detailHistogram.value = null
    }

    fun sendPhotoToWatermarkStudio(photo: CameraPhoto, loadedBitmap: Bitmap?, exif: PhotoExif?) {
        closePhotoDetail()
        _activeTab.value = AppTab.WATERMARK
        viewModelScope.launch {
            _watermarkState.update { it.copy(isRendering = true) }
            val bmp = loadedBitmap ?: repo.loadPreviewBitmap(photo).getOrNull()
            val resolvedExif = exif ?: repo.fetchPhotoExif(photo)
            if (bmp != null) {
                _watermarkState.update { state ->
                    state.copy(
                        sourceBitmap = bmp,
                        sourceFileName = photo.file,
                        config = WatermarkConfig.fromExif(resolvedExif, state.config)
                    )
                }
                triggerWatermarkPreview()
            } else {
                _watermarkState.update { it.copy(isRendering = false) }
                showToast("无法加载该照片用于水印编辑")
            }
        }
    }

    // ---------------------------------------------------------
    // Transfer Queue & Foreground Service
    // ---------------------------------------------------------
    fun setDefaultQuality(quality: DownloadQuality) {
        _defaultQuality.value = quality
    }

    fun setAutoWatermarkOnTransfer(enabled: Boolean) {
        _autoWatermarkOnTransfer.value = enabled
    }

    fun enqueueDownload(photosToDownload: List<CameraPhoto>, quality: DownloadQuality = _defaultQuality.value) {
        if (photosToDownload.isEmpty()) return
        val newTasks = photosToDownload.map { photo ->
            val effectiveQuality = if (photo.isRaw && quality == DownloadQuality.FULL_JPG) {
                DownloadQuality.RAW
            } else {
                quality
            }
            TransferTask(
                id = UUID.randomUUID().toString(),
                photo = photo,
                quality = effectiveQuality,
                autoWatermark = _autoWatermarkOnTransfer.value && !photo.isRaw
            )
        }
        _transferTasks.update { newTasks + it }
        _selectedPhotoIds.value = emptySet()
        showToast("已添加 ${newTasks.size} 张照片至后台传输队列")
        ensureTransferWorkerRunning()
    }

    fun enqueueOnlyUndownloaded() {
        val undownloaded = _photos.value.filter { !it.isDownloaded }
        if (undownloaded.isEmpty()) {
            showToast("当前存储卡内所有照片均已下载")
            return
        }
        enqueueDownload(undownloaded, _defaultQuality.value)
    }

    fun clearCompletedTasks() {
        _transferTasks.update { list ->
            list.filter { it.status == TransferStatus.QUEUED || it.status == TransferStatus.DOWNLOADING }
        }
    }

    fun cancelAllPendingTasks() {
        transferWorkerJob?.cancel()
        _transferTasks.update { list ->
            list.map {
                if (it.status == TransferStatus.QUEUED || it.status == TransferStatus.DOWNLOADING) {
                    it.copy(status = TransferStatus.CANCELLED)
                } else it
            }
        }
        TransferForegroundService.stop(getApplication())
    }

    private fun ensureTransferWorkerRunning() {
        if (transferWorkerJob?.isActive == true) return
        transferWorkerJob = viewModelScope.launch(Dispatchers.IO) {
            val ctx = getApplication<Application>()
            while (isActive) {
                val next = _transferTasks.value.lastOrNull { it.status == TransferStatus.QUEUED }
                    ?: break

                updateTask(next.id) { it.copy(status = TransferStatus.DOWNLOADING, progress = 0f) }
                val remainingCount = _transferTasks.value.count {
                    it.status == TransferStatus.QUEUED || it.status == TransferStatus.DOWNLOADING
                }
                TransferForegroundService.updateProgress(
                    ctx,
                    title = "K-1 传图中 (剩余 $remainingCount 张)",
                    detail = "${next.photo.file} · ${next.quality.label}",
                    progress = 0,
                    max = 100
                )

                val res = repo.downloadPhotoBytes(next.photo, next.quality) { read, total ->
                    val frac = if (total > 0) (read.toFloat() / total).coerceIn(0f, 1f) else 0.5f
                    updateTask(next.id) {
                        it.copy(progress = frac, bytesRead = read, totalBytes = total)
                    }
                    TransferForegroundService.updateProgress(
                        ctx,
                        title = "K-1 传图中 (剩余 $remainingCount 张)",
                        detail = "${next.photo.file} (${(frac * 100).toInt()}%)",
                        progress = (frac * 100).toInt(),
                        max = 100
                    )
                }

                res.onSuccess { bytes ->
                    val isRawFile = next.photo.isRaw && next.quality == DownloadQuality.RAW
                    val outName = if (next.quality == DownloadQuality.VIEW && next.photo.isRaw) {
                        next.photo.file.substringBeforeLast('.') + "_preview.jpg"
                    } else {
                        next.photo.file
                    }

                    val saveResult = MediaStoreSaver.saveImageBytes(
                        context = ctx,
                        bytes = bytes,
                        fileName = outName,
                        isRaw = isRawFile
                    )

                    // Optional auto-watermark copy
                    if (next.autoWatermark && !isRawFile) {
                        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (bmp != null) {
                            val exif = repo.fetchPhotoExif(next.photo)
                            val cfg = WatermarkConfig.fromExif(exif, _watermarkState.value.config)
                            val marked = PentaxWatermarkRenderer.render(bmp, cfg)
                            val wmName = outName.substringBeforeLast('.') + "_pentax_wm.jpg"
                            MediaStoreSaver.saveBitmapAsJpeg(ctx, marked, wmName)
                            marked.recycle()
                            bmp.recycle()
                        }
                    }

                    saveResult.onSuccess { savedPath ->
                        repo.historyStore.markDownloaded(next.photo.uniqueId)
                        _photos.update { list ->
                            list.map { p ->
                                if (p.uniqueId == next.photo.uniqueId) p.copy(isDownloaded = true) else p
                            }
                        }
                        updateTask(next.id) {
                            it.copy(
                                status = TransferStatus.COMPLETED,
                                progress = 1f,
                                savedPath = savedPath
                            )
                        }
                    }.onFailure { err ->
                        updateTask(next.id) {
                            it.copy(status = TransferStatus.FAILED, errorMessage = err.localizedMessage)
                        }
                    }
                }.onFailure { err ->
                    updateTask(next.id) {
                        it.copy(status = TransferStatus.FAILED, errorMessage = err.localizedMessage)
                    }
                }
            }
            TransferForegroundService.stop(ctx)
        }
    }

    private fun updateTask(id: String, transform: (TransferTask) -> TransferTask) {
        _transferTasks.update { list ->
            list.map { if (it.id == id) transform(it) else it }
        }
    }

    // ---------------------------------------------------------
    // Remote Control, Live View, Intervalometer, Bulb, Tether
    // ---------------------------------------------------------
    fun startLiveView() {
        if (!_cameraInfo.value.connected) return
        stopLiveView()
        _liveViewActive.value = true
        _liveViewError.value = null
        liveViewJob = viewModelScope.launch {
            if (_isMockMode.value) {
                var tick = 0
                while (isActive) {
                    val frame = withContext(Dispatchers.Default) {
                        repo.mockRepo.renderLiveViewFrame(tick++)
                    }
                    _liveViewFrame.value = frame
                    delay(120)
                }
            } else {
                repo.apiClient.streamLiveView(
                    onFrame = { bmp -> _liveViewFrame.value = bmp },
                    onError = { msg ->
                        _liveViewError.value = msg
                        _liveViewActive.value = false
                    }
                )
            }
        }
    }

    fun stopLiveView() {
        liveViewJob?.cancel()
        liveViewJob = null
        _liveViewActive.value = false
    }

    fun setManualLensMode(enabled: Boolean) {
        _manualLensMode.value = enabled
        showToast(if (enabled) "手动头安全快门已开启（跳过 AF 立即释放）" else "已恢复自动对焦快门")
    }

    fun setGridMode(mode: GridOverlayMode) {
        _gridMode.value = mode
    }

    fun setRemoteShotMode(mode: RemoteShotMode) {
        _remoteShotMode.value = mode
    }

    fun updateCameraParam(key: String, value: String) {
        viewModelScope.launch {
            repo.updateParam(key, value)
                .onSuccess { updatedMock ->
                    if (updatedMock != null) {
                        _shootingParams.value = updatedMock
                    } else {
                        _shootingParams.update { cur ->
                            when (key) {
                                "av" -> cur.copy(av = value)
                                "tv" -> cur.copy(tv = value)
                                "sv" -> cur.copy(sv = value)
                                "xv" -> cur.copy(xv = value)
                                "wbMode" -> cur.copy(wbMode = value)
                                else -> cur
                            }
                        }
                    }
                }
                .onFailure { err ->
                    showToast("调整参数失败: ${err.localizedMessage}")
                }
        }
    }

    fun touchFocus(xPercent: Int, yPercent: Int) {
        if (_manualLensMode.value) {
            showToast("当前为手动头模式（AF 已关闭）")
            return
        }
        viewModelScope.launch {
            repo.triggerAutoFocus(xPercent, yPercent)
                .onFailure { showToast("对焦未成功: ${it.localizedMessage}") }
        }
    }

    fun shootSingle() {
        if (!_cameraInfo.value.connected) {
            showToast("请先连接 PENTAX K-1")
            return
        }
        viewModelScope.launch {
            repo.triggerShutter(afEnabled = !_manualLensMode.value)
                .onSuccess { mockPhoto ->
                    showToast("快门已释放")
                    if (mockPhoto != null) {
                        refreshPhotos()
                        if (_tetherEnabled.value) {
                            enqueueDownload(listOf(mockPhoto), DownloadQuality.VIEW)
                        }
                    }
                }
                .onFailure { err ->
                    showToast("快门失败: ${err.localizedMessage}")
                }
        }
    }

    fun updateIntervalConfig(intervalSec: Int, totalShots: Int) {
        _intervalState.update {
            it.copy(
                intervalSec = intervalSec.coerceIn(2, 3600),
                totalShots = totalShots.coerceIn(2, 999)
            )
        }
    }

    fun toggleIntervalometer() {
        if (_intervalState.value.running) {
            intervalJob?.cancel()
            _intervalState.update { it.copy(running = false, nextShotCountdown = 0) }
            showToast("已停止间隔延时拍摄")
            return
        }
        if (!_cameraInfo.value.connected) {
            showToast("请先连接 PENTAX K-1")
            return
        }
        val cfg = _intervalState.value
        _intervalState.update { it.copy(running = true, shotsTaken = 0, nextShotCountdown = 0) }
        intervalJob = viewModelScope.launch {
            for (shot in 1..cfg.totalShots) {
                if (!isActive) break
                repo.triggerShutter(afEnabled = !_manualLensMode.value)
                _intervalState.update { it.copy(shotsTaken = shot) }
                if (shot < cfg.totalShots) {
                    for (sec in cfg.intervalSec downTo 1) {
                        if (!isActive) break
                        _intervalState.update { it.copy(nextShotCountdown = sec) }
                        delay(1000)
                    }
                }
            }
            _intervalState.update { it.copy(running = false, nextShotCountdown = 0) }
            showToast("间隔延时拍摄完成 (${cfg.totalShots} 张)")
            refreshPhotos()
        }
    }

    fun updateBulbTarget(seconds: Int) {
        _bulbState.update { it.copy(targetSec = seconds.coerceIn(5, 7200)) }
    }

    fun toggleBulbExposure() {
        if (_bulbState.value.running) {
            bulbJob?.cancel()
            viewModelScope.launch {
                repo.finishBulb()
                _bulbState.update { it.copy(running = false) }
                showToast("B门长曝已手动结束 (${_bulbState.value.elapsedSec}s)")
                refreshPhotos()
            }
            return
        }
        if (!_cameraInfo.value.connected) {
            showToast("请先连接 PENTAX K-1")
            return
        }
        val target = _bulbState.value.targetSec
        bulbJob = viewModelScope.launch {
            repo.startBulb().onFailure {
                showToast("B门启动失败（请确认机身拨盘处于 B 门）: ${it.localizedMessage}")
                return@launch
            }
            _bulbState.update { it.copy(running = true, elapsedSec = 0) }
            for (sec in 1..target) {
                if (!isActive) break
                delay(1000)
                _bulbState.update { it.copy(elapsedSec = sec) }
            }
            repo.finishBulb()
            _bulbState.update { it.copy(running = false) }
            showToast("B门长曝完成 (${target}s)")
            refreshPhotos()
        }
    }

    fun toggleTetherMode(enabled: Boolean) {
        _tetherEnabled.value = enabled
        if (enabled && _cameraInfo.value.connected && !_isMockMode.value) {
            repo.apiClient.startChangesListener(
                onNewPhoto = { dir, file, slot ->
                    val upper = file.uppercase()
                    val isRaw = upper.endsWith(".PEF") || upper.endsWith(".DNG")
                    val photo = CameraPhoto(
                        dir = dir,
                        file = file,
                        slot = slot,
                        dateGroup = "最新拍摄",
                        isRaw = isRaw
                    )
                    viewModelScope.launch {
                        enqueueDownload(listOf(photo), DownloadQuality.VIEW)
                    }
                },
                onStateChanged = {
                    viewModelScope.launch {
                        repo.apiClient.fetchCameraParams(_shootingParams.value)
                            .onSuccess { _shootingParams.value = it }
                    }
                }
            )
            showToast("拍完即传已开启：按下快门后将自动回传预览图")
        } else if (!enabled) {
            stopTether()
        } else if (enabled && _isMockMode.value) {
            showToast("拍完即传已开启（模拟模式）")
        }
    }

    private fun stopTether() {
        _tetherEnabled.value = false
        repo.apiClient.stopChangesListener()
    }

    // ---------------------------------------------------------
    // Pentax Watermark Studio
    // ---------------------------------------------------------
    fun updateWatermarkConfig(transform: (WatermarkConfig) -> WatermarkConfig) {
        _watermarkState.update { it.copy(config = transform(it.config)) }
        triggerWatermarkPreview()
    }

    fun loadLocalPhotoForWatermark(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _watermarkState.update { it.copy(isRendering = true) }
            val ctx = getApplication<Application>()
            try {
                val rawBytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("无法读取本地图片")
                val bmp = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
                    ?: error("无法解析图片格式")

                // Extract real EXIF from the selected local image (supports K-1 JPGs!)
                val exifInterface = ExifInterface(ByteArrayInputStream(rawBytes))
                val model = exifInterface.getAttribute(ExifInterface.TAG_MODEL)?.trim().orEmpty()
                val focal = parseExifFocal(exifInterface.getAttribute(ExifInterface.TAG_FOCAL_LENGTH))
                val aperture = exifInterface.getAttribute(ExifInterface.TAG_F_NUMBER)?.trim().orEmpty()
                val shutter = parseExifShutter(exifInterface.getAttribute(ExifInterface.TAG_EXPOSURE_TIME))
                val iso = exifInterface.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
                    ?: exifInterface.getAttribute("ISOSpeedRatings")
                    ?: ""
                val dt = exifInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?.replaceFirst(":", ".")?.replaceFirst(":", ".")
                    ?: ""
                val lens = exifInterface.getAttribute(ExifInterface.TAG_LENS_MODEL)?.trim().orEmpty()

                val parsedExif = PhotoExif(
                    cameraModel = model.ifBlank { "PENTAX K-1" },
                    lensModel = lens,
                    focalLength = focal,
                    aperture = aperture,
                    shutterSpeed = shutter,
                    iso = iso,
                    dateTime = dt
                )

                _watermarkState.update { state ->
                    state.copy(
                        sourceBitmap = bmp,
                        sourceFileName = "PENTAX_LOCAL_${System.currentTimeMillis() % 10000}.jpg",
                        config = WatermarkConfig.fromExif(parsedExif, state.config)
                    )
                }
                triggerWatermarkPreview()
            } catch (e: Exception) {
                _watermarkState.update { it.copy(isRendering = false) }
                showToast("读取相册照片失败: ${e.localizedMessage}")
            }
        }
    }

    fun triggerWatermarkPreview() {
        val src = _watermarkState.value.sourceBitmap ?: return
        val cfg = _watermarkState.value.config
        watermarkRenderJob?.cancel()
        watermarkRenderJob = viewModelScope.launch {
            _watermarkState.update { it.copy(isRendering = true) }
            val rendered = PentaxWatermarkRenderer.render(src, cfg, maxLongEdge = 1280)
            _watermarkState.update { it.copy(previewBitmap = rendered, isRendering = false) }
        }
    }

    fun exportWatermarkedPhoto() {
        val state = _watermarkState.value
        val src = state.sourceBitmap ?: run {
            showToast("请先选择一张照片")
            return
        }
        viewModelScope.launch {
            _watermarkState.update { it.copy(isSaving = true) }
            val maxEdge = if (state.config.highQualityExport) null else 2560
            val finalBitmap = PentaxWatermarkRenderer.render(src, state.config, maxLongEdge = maxEdge)
            val baseName = state.sourceFileName.substringBeforeLast('.')
            val outName = "${baseName}_PENTAX_${state.config.style.name.lowercase()}.jpg"
            val result = withContext(Dispatchers.IO) {
                MediaStoreSaver.saveBitmapAsJpeg(
                    context = getApplication(),
                    bitmap = finalBitmap,
                    fileName = outName,
                    quality = if (state.config.highQualityExport) 97 else 90,
                    subFolder = "PentaxK1_Watermark"
                )
            }
            finalBitmap.recycle()
            _watermarkState.update { it.copy(isSaving = false) }
            result.onSuccess { path ->
                showToast("宾得水印照已导出至：$path")
            }.onFailure { err ->
                showToast("导出失败: ${err.localizedMessage}")
            }
        }
    }

    private fun parseExifFocal(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return try {
            if (raw.contains("/")) {
                val parts = raw.split("/")
                val num = parts[0].toDouble()
                val den = parts[1].toDouble()
                val mm = if (den != 0.0) num / den else num
                if (mm % 1.0 == 0.0) "${mm.toInt()}mm" else "${"%.1f".format(mm)}mm"
            } else {
                "${raw.toDouble().toInt()}mm"
            }
        } catch (_: Exception) {
            raw
        }
    }

    private fun parseExifShutter(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return try {
            val sec = raw.toDouble()
            if (sec >= 1.0) {
                if (sec % 1.0 == 0.0) "${sec.toInt()}s" else "${"%.1f".format(sec)}s"
            } else if (sec > 0.0) {
                "1/${(1.0 / sec).toInt()}s"
            } else {
                raw
            }
        } catch (_: Exception) {
            raw
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLiveView()
        stopTether()
        repo.wifiBinder.releaseBinding()
    }
}

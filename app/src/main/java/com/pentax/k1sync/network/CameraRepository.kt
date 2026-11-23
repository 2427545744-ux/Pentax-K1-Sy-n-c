package com.pentax.k1sync.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.pentax.k1sync.cache.DownloadHistoryStore
import com.pentax.k1sync.cache.ThumbnailDiskCache
import com.pentax.k1sync.model.CameraInfo
import com.pentax.k1sync.model.CameraPhoto
import com.pentax.k1sync.model.DownloadQuality
import com.pentax.k1sync.model.PhotoExif
import com.pentax.k1sync.model.ShootingParams
import com.pentax.k1sync.model.StorageSlot
import kotlinx.coroutines.delay

class CameraRepository(
    val wifiBinder: WifiNetworkBinder,
    val apiClient: PentaxK1ApiClient,
    val mockRepo: MockPentaxK1Repository,
    val thumbCache: ThumbnailDiskCache,
    val historyStore: DownloadHistoryStore
) {
    @Volatile
    var isMockMode: Boolean = false

    suspend fun connectAndFetchStatus(): Result<Pair<CameraInfo, ShootingParams>> {
        return if (isMockMode) {
            delay(250)
            Result.success(mockRepo.getCameraInfo() to mockRepo.getParams())
        } else {
            apiClient.pingAndFetchProps()
        }
    }

    suspend fun listPhotos(slot: StorageSlot): Result<List<CameraPhoto>> {
        val downloaded = historyStore.getDownloadedIds()
        val rawResult = if (isMockMode) {
            delay(180)
            Result.success(mockRepo.listPhotos(slot))
        } else {
            apiClient.listPhotos(slot)
        }
        return rawResult.map { list ->
            list.map { photo -> photo.copy(isDownloaded = downloaded.contains(photo.uniqueId)) }
        }
    }

    suspend fun loadThumbnail(photo: CameraPhoto): Bitmap? {
        val cacheKey = "${if (isMockMode) "mock" else "real"}_${photo.uniqueId}_thumb"
        thumbCache.get(cacheKey)?.let { return it }

        val bytes = if (isMockMode) {
            mockRepo.renderMockPhotoBytes(photo, 360, 240)
        } else {
            apiClient.downloadPhotoBytes(photo, "thumb").getOrNull()
        } ?: return null

        return thumbCache.put(cacheKey, bytes)
    }

    suspend fun loadPreviewBitmap(photo: CameraPhoto): Result<Bitmap> {
        val bytes = if (isMockMode) {
            delay(150)
            Result.success(mockRepo.renderMockPhotoBytes(photo, 1620, 1080))
        } else {
            apiClient.downloadPhotoBytes(photo, "view")
        }
        return bytes.mapCatching { data ->
            BitmapFactory.decodeByteArray(data, 0, data.size) ?: error("无法解码图像数据")
        }
    }

    suspend fun fetchPhotoExif(photo: CameraPhoto): PhotoExif {
        if (isMockMode) {
            return photo.exif ?: PhotoExif(cameraModel = "PENTAX K-1")
        }
        return apiClient.fetchPhotoInfo(photo).getOrElse {
            photo.exif ?: PhotoExif(cameraModel = "PENTAX K-1")
        }
    }

    suspend fun downloadPhotoBytes(
        photo: CameraPhoto,
        quality: DownloadQuality,
        onProgress: (Long, Long) -> Unit
    ): Result<ByteArray> {
        return if (isMockMode) {
            val total = when (quality) {
                DownloadQuality.VIEW -> 800_000L
                DownloadQuality.FULL_JPG -> 12_000_000L
                DownloadQuality.RAW -> 44_000_000L
            }
            for (step in 1..6) {
                delay(120)
                onProgress((total * step) / 6, total)
            }
            val w = if (quality == DownloadQuality.VIEW) 1620 else 2400
            val h = if (quality == DownloadQuality.VIEW) 1080 else 1600
            Result.success(mockRepo.renderMockPhotoBytes(photo, w, h))
        } else {
            val sizeParam = when (quality) {
                DownloadQuality.VIEW -> "view"
                DownloadQuality.FULL_JPG, DownloadQuality.RAW -> "full"
            }
            apiClient.downloadPhotoBytes(photo, sizeParam, onProgress)
        }
    }

    suspend fun updateParam(key: String, value: String): Result<ShootingParams?> {
        return if (isMockMode) {
            Result.success(mockRepo.setParam(key, value))
        } else {
            apiClient.updateCameraParam(key, value).map { null }
        }
    }

    suspend fun triggerAutoFocus(xPercent: Int?, yPercent: Int?): Result<Unit> {
        return if (isMockMode) {
            delay(180)
            Result.success(Unit)
        } else {
            apiClient.triggerAutoFocus(xPercent, yPercent)
        }
    }

    suspend fun triggerShutter(afEnabled: Boolean): Result<CameraPhoto?> {
        return if (isMockMode) {
            delay(220)
            Result.success(mockRepo.shootNewPhoto())
        } else {
            apiClient.triggerShutter(afEnabled).map { null }
        }
    }

    suspend fun startBulb(): Result<Unit> =
        if (isMockMode) Result.success(Unit) else apiClient.startBulbExposure()

    suspend fun finishBulb(): Result<Unit> =
        if (isMockMode) Result.success(Unit) else apiClient.finishBulbExposure()

    suspend fun syncPhoneTime(): Result<String> =
        if (isMockMode) Result.success("2026-11-22T19:25:00 (Mock)") else apiClient.syncPhoneTimeToCamera()
}

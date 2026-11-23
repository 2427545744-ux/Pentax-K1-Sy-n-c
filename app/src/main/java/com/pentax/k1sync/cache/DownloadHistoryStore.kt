package com.pentax.k1sync.cache

import android.content.Context

class DownloadHistoryStore(context: Context) {

    private val prefs = context.getSharedPreferences("k1_sync_history", Context.MODE_PRIVATE)
    private val keySet = "downloaded_photo_ids"

    fun getDownloadedIds(): Set<String> {
        return prefs.getStringSet(keySet, emptySet())?.toSet() ?: emptySet()
    }

    fun markDownloaded(uniqueId: String) {
        val updated = getDownloadedIds().toMutableSet()
        updated.add(uniqueId)
        prefs.edit().putStringSet(keySet, updated).apply()
    }

    fun isDownloaded(uniqueId: String): Boolean = getDownloadedIds().contains(uniqueId)

    fun clearHistory() {
        prefs.edit().remove(keySet).apply()
    }
}

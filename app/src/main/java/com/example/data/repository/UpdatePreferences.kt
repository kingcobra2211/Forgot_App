package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences

class UpdatePreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("forgot_update_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DOWNLOADED_VERSION = "downloaded_version"
        private const val KEY_DOWNLOADED_APK_PATH = "downloaded_apk_path"
    }

    fun getDownloadedVersion(): String? {
        return prefs.getString(KEY_DOWNLOADED_VERSION, null)
    }

    fun getDownloadedApkPath(): String? {
        return prefs.getString(KEY_DOWNLOADED_APK_PATH, null)
    }

    fun saveDownloadInfo(version: String, path: String) {
        prefs.edit()
            .putString(KEY_DOWNLOADED_VERSION, version)
            .putString(KEY_DOWNLOADED_APK_PATH, path)
            .apply()
    }

    fun clearDownloadInfo() {
        prefs.edit()
            .remove(KEY_DOWNLOADED_VERSION)
            .remove(KEY_DOWNLOADED_APK_PATH)
            .apply()
    }
}

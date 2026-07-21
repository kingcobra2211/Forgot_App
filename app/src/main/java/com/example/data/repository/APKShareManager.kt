package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class APKShareManager(private val context: Context) {

    /**
     * Copies currently installed APK to private app cache and initiates the system Share Sheet.
     */
    fun shareInstalledAPK(versionName: String): Result<Unit> {
        return try {
            val publicSourceDir = context.applicationInfo.publicSourceDir
            val sourceFile = File(publicSourceDir)
            if (!sourceFile.exists()) {
                return Result.failure(Exception("Base APK file not found in system directories."))
            }

            // Create 'export' directory matching <cache-path name="shared_apk" path="export/" />
            val exportDir = File(context.cacheDir, "export")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            } else {
                // Remove previous temporary APKs to save storage space
                exportDir.listFiles()?.forEach { file ->
                    try {
                        file.delete()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            val safeVersion = versionName.replace(" ", "_")
            val destFileName = "Forgot_v$safeVersion.apk"
            val destFile = File(exportDir, destFileName)

            // Copy base APK into cache
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (!destFile.exists() || destFile.length() == 0L) {
                return Result.failure(Exception("Failed to copy APK into share folder."))
            }

            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, destFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Forgot App APK")
                putExtra(Intent.EXTRA_TEXT, "Here is the direct installation APK for Forgot - Save it once. Forget nothing.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Forgot APK via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

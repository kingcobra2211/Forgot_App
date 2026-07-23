package com.example.data.repository

import android.content.Context
import com.example.BuildConfig
import com.example.data.model.ReleaseInfo
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import java.util.concurrent.TimeUnit

interface UpdateService {
    @GET("repos/vamshivamshi9630/Forgot_App_Latest_Versions/releases/latest")
    suspend fun getLatestRelease(
        @Header("User-Agent") userAgent: String = "Forgot-App-OTA"
    ): ReleaseInfo
}

class UpdateRepository(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("forgot_prefs", Context.MODE_PRIVATE)

    private val updateService: UpdateService by lazy {
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(UpdateService::class.java)
    }

    suspend fun fetchLatestRelease(): Result<ReleaseInfo> {
        return try {
            val release = updateService.getLatestRelease()
            Result.success(release)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentVersion(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    fun getBuildNumber(): Long {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            @Suppress("DEPRECATION")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                pInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            1L
        }
    }

    fun getSkippedVersion(): String? {
        return sharedPrefs.getString("skipped_update_version", null)
    }

    fun setSkippedVersion(version: String?) {
        sharedPrefs.edit().putString("skipped_update_version", version).apply()
    }

    fun isAutoCheckOnStartup(): Boolean {
        // Default to true for premium experience
        return sharedPrefs.getBoolean("auto_check_on_startup", true)
    }

    fun setAutoCheckOnStartup(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("auto_check_on_startup", enabled).apply()
    }

    fun getLastCheckedTime(): Long {
        return sharedPrefs.getLong("last_checked_update_time", 0L)
    }

    fun setLastCheckedTime(time: Long) {
        sharedPrefs.edit().putLong("last_checked_update_time", time).apply()
    }
}

package com.b_lam.resplash.service

import android.app.Notification
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.CallSuper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ServiceLifecycleDispatcher
import androidx.lifecycle.observe
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.b_lam.resplash.R
import com.b_lam.resplash.domain.SharedPreferencesRepository
import com.b_lam.resplash.ui.autowallpaper.AutoWallpaperSettingsActivity
import com.b_lam.resplash.util.createNotification
import com.b_lam.resplash.worker.AutoWallpaperWorker
import org.koin.core.KoinComponent
import org.koin.core.get
import org.koin.core.inject

@RequiresApi(Build.VERSION_CODES.N)
class AutoWallpaperTileService: TileService(), LifecycleOwner, KoinComponent {

    private val dispatcher = ServiceLifecycleDispatcher(this)

    override fun onClick() {
        qsTile?.let { tile ->
            when (tile.state) {
                Tile.STATE_ACTIVE -> {
                    AutoWallpaperWorker.scheduleSingleAutoWallpaperJob(this@AutoWallpaperTileService, get())
                }
                else -> unlockAndRun {
                    Intent(this@AutoWallpaperTileService, AutoWallpaperSettingsActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivityAndCollapse(this)
                    }
                }
            }
        }
    }

    override fun onStartListening() {
        val sharedPreferencesRepository: SharedPreferencesRepository by inject()
        qsTile.apply {
            if (sharedPreferencesRepository.autoWallpaperEnabled) {
                state = Tile.STATE_ACTIVE
                label = getString(R.string.auto_wallpaper_next_wallpaper)
                icon = Icon.createWithResource(this@AutoWallpaperTileService, R.drawable.ic_skip_next_24dp)
            } else {
                state = Tile.STATE_INACTIVE
                label = getString(R.string.auto_wallpaper_activate)
                icon = Icon.createWithResource(this@AutoWallpaperTileService, R.drawable.ic_compare_24dp)
            }
            updateTile()
        }
        WorkManager.getInstance(this@AutoWallpaperTileService)
            .getWorkInfosForUniqueWorkLiveData(AutoWallpaperWorker.AUTO_WALLPAPER_SINGLE_JOB_ID)
            .observe(this@AutoWallpaperTileService) {
                if (it.isNotEmpty()) {
                    when (it?.first()?.state) {
                        WorkInfo.State.BLOCKED, WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING -> {
                            val notification = applicationContext.createNotification(NotificationCompat.PRIORITY_LOW) {
                                setContentTitle(getString(R.string.setting_wallpaper))
                                setProgress(0, 0, true)
                                setTimeoutAfter(60000)
                            }
                            showNotification(notification)
                        }
                        WorkInfo.State.SUCCEEDED -> cancelNotification()
                        WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> {
                            cancelNotification()
                            val notification = applicationContext.createNotification {
                                setContentTitle(getString(R.string.error_setting_wallpaper))
                            }
                            showNotification(notification)
                        }
                    }
                }
            }
    }

    @CallSuper
    override fun onCreate() {
        dispatcher.onServicePreSuperOnCreate()
        super.onCreate()
    }

    @CallSuper
    override fun onBind(intent: Intent): IBinder? {
        dispatcher.onServicePreSuperOnBind()
        return super.onBind(intent)
    }

    @Suppress("DEPRECATION")
    @CallSuper
    override fun onStart(intent: Intent?, startId: Int) {
        dispatcher.onServicePreSuperOnStart()
        super.onStart(intent, startId)
    }

    @CallSuper
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    @CallSuper
    override fun onDestroy() {
        dispatcher.onServicePreSuperOnDestroy()
        cancelNotification()
        super.onDestroy()
    }

    override fun getLifecycle() = dispatcher.lifecycle

    private fun showNotification(notification: Notification) =
        with(NotificationManagerCompat.from(applicationContext)) {
            notify(AUTO_WALLPAPER_TILE_SERVICE_NOTIFICATION_ID, notification)
        }

    private fun cancelNotification() = with(NotificationManagerCompat.from(applicationContext)) {
        cancel(AUTO_WALLPAPER_TILE_SERVICE_NOTIFICATION_ID)
    }

    companion object {

        private const val AUTO_WALLPAPER_TILE_SERVICE_NOTIFICATION_ID = 444
    }
}
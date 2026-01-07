package com.freewhiteboard.animator

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.freewhiteboard.animator.data.database.AppDatabase
import com.freewhiteboard.animator.data.repository.ProjectRepository
import com.freewhiteboard.animator.engine.AssetManager
import com.freewhiteboard.animator.engine.TTSManager

/**
 * Application class for Whiteboard Animator.
 * Provides access to singletons and initializes app-wide resources.
 */
class WhiteboardAnimatorApp : Application() {
    
    // Database instance
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    
    // Repository instance
    val repository: ProjectRepository by lazy {
        ProjectRepository(
            projectDao = database.projectDao(),
            sceneDao = database.sceneDao(),
            sceneAssetDao = database.sceneAssetDao()
        )
    }
    
    // Asset manager instance
    val assetManager: AssetManager by lazy { AssetManager(this) }
    
    // TTS manager instance
    val ttsManager: TTSManager by lazy { TTSManager(this) }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }
    
    /**
     * Create notification channels for export progress.
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val exportChannel = NotificationChannel(
                CHANNEL_EXPORT,
                getString(R.string.notification_channel_export),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_export_desc)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(exportChannel)
        }
    }
    
    companion object {
        const val CHANNEL_EXPORT = "export_channel"
        
        @Volatile
        private var instance: WhiteboardAnimatorApp? = null
        
        fun getInstance(): WhiteboardAnimatorApp {
            return instance ?: throw IllegalStateException("Application not initialized")
        }
    }
}

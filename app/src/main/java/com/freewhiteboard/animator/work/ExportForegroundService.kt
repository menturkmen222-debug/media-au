package com.freewhiteboard.animator.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.freewhiteboard.animator.R
import com.freewhiteboard.animator.WhiteboardAnimatorApp

/**
 * Foreground service for video export to keep task running.
 */
class ExportForegroundService : Service() {
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = WhiteboardAnimatorApp.CHANNEL_EXPORT
        
        fun start(context: Context, projectName: String) {
            val intent = Intent(context, ExportForegroundService::class.java).apply {
                putExtra("project_name", projectName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stop(context: Context) {
            context.stopService(Intent(context, ExportForegroundService::class.java))
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val projectName = intent?.getStringExtra("project_name") ?: "Video"
        
        createNotificationChannel()
        val notification = createNotification(projectName, 0)
        startForeground(NOTIFICATION_ID, notification)
        
        return START_NOT_STICKY
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_export),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_export_desc)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(projectName: String, progress: Int): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.export_progress))
            .setContentText("Exporting \"$projectName\"...")
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .build()
    }
    
    fun updateProgress(progress: Int) {
        val notification = createNotification("Video", progress)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}

package com.freewhiteboard.animator.work

import android.content.Context
import androidx.work.*
import com.freewhiteboard.animator.WhiteboardAnimatorApp
import com.freewhiteboard.animator.data.model.Resolution
import com.freewhiteboard.animator.data.model.AspectRatio
import com.freewhiteboard.animator.engine.SceneBitmaps
import com.freewhiteboard.animator.engine.VideoExporter
import java.io.File
import java.util.UUID

/**
 * WorkManager worker for background video export.
 * Runs video encoding in the background with progress updates.
 */
class ExportWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    companion object {
        const val KEY_PROJECT_ID = "project_id"
        const val KEY_PROJECT_NAME = "project_name"
        const val KEY_OUTPUT_PATH = "output_path"
        const val KEY_RESOLUTION = "resolution"
        const val KEY_ASPECT_RATIO = "aspect_ratio"
        const val KEY_PROGRESS = "progress"
        const val KEY_RESULT_PATH = "result_path"
        
        /**
         * Create and enqueue export work.
         */
        fun enqueue(
            context: Context,
            projectId: Long,
            projectName: String,
            outputPath: String,
            resolution: Resolution,
            aspectRatio: AspectRatio
        ): UUID {
            val workRequest = OneTimeWorkRequestBuilder<ExportWorker>()
                .setInputData(workDataOf(
                    KEY_PROJECT_ID to projectId,
                    KEY_PROJECT_NAME to projectName,
                    KEY_OUTPUT_PATH to outputPath,
                    KEY_RESOLUTION to resolution.name,
                    KEY_ASPECT_RATIO to aspectRatio.name
                ))
                .addTag("export_$projectId")
                .build()
            
            WorkManager.getInstance(context).enqueue(workRequest)
            return workRequest.id
        }
        
        /**
         * Get work info LiveData for observing progress.
         */
        fun getWorkInfo(context: Context, workId: UUID) =
            WorkManager.getInstance(context).getWorkInfoByIdFlow(workId)
    }
    
    override suspend fun doWork(): Result {
        val projectId = inputData.getLong(KEY_PROJECT_ID, -1)
        val projectName = inputData.getString(KEY_PROJECT_NAME) ?: "Video"
        val outputPath = inputData.getString(KEY_OUTPUT_PATH) ?: return Result.failure()
        val resolutionName = inputData.getString(KEY_RESOLUTION) ?: Resolution.HD_720P.name
        val aspectRatioName = inputData.getString(KEY_ASPECT_RATIO) ?: AspectRatio.HORIZONTAL_16_9.name
        
        if (projectId < 0) return Result.failure()
        
        val resolution = Resolution.valueOf(resolutionName)
        val aspectRatio = AspectRatio.valueOf(aspectRatioName)
        
        try {
            // Start foreground service for notification
            ExportForegroundService.start(applicationContext, projectName)
            
            val app = WhiteboardAnimatorApp.getInstance()
            val scenes = app.repository.getScenes(projectId)
            
            if (scenes.isEmpty()) {
                return Result.failure()
            }
            
            // Get assets for all scenes
            val assets = scenes.associate { scene ->
                scene.id to app.repository.getAssets(scene.id)
            }
            
            val videoExporter = VideoExporter(applicationContext)
            
            val result = videoExporter.exportVideo(
                scenes = scenes,
                assets = assets,
                bitmaps = SceneBitmaps(),
                outputPath = outputPath,
                resolution = resolution,
                aspectRatio = aspectRatio,
                onProgress = { progress ->
                    setProgressAsync(workDataOf(KEY_PROGRESS to (progress * 100).toInt()))
                }
            )
            
            // Stop foreground service
            ExportForegroundService.stop(applicationContext)
            
            return result.fold(
                onSuccess = { path ->
                    Result.success(workDataOf(KEY_RESULT_PATH to path))
                },
                onFailure = {
                    Result.failure()
                }
            )
        } catch (e: Exception) {
            ExportForegroundService.stop(applicationContext)
            return Result.failure()
        }
    }
}

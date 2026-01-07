package com.freewhiteboard.animator.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.freewhiteboard.animator.R
import com.freewhiteboard.animator.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages built-in assets and user images.
 * Provides access to hands, backgrounds, and doodles.
 */
class AssetManager(private val context: Context) {
    
    /**
     * All available hand styles.
     */
    val availableHands: List<AssetInfo> = listOf(
        AssetInfo("hand_pointing", "Pointing Hand", AssetCategory.HAND, R.drawable.hand_pointing),
        AssetInfo("hand_writing", "Writing Hand", AssetCategory.HAND, R.drawable.hand_writing),
        AssetInfo("hand_cartoon", "Cartoon Hand", AssetCategory.HAND, R.drawable.hand_cartoon)
    )
    
    /**
     * All available backgrounds.
     */
    val availableBackgrounds: List<AssetInfo> = listOf(
        AssetInfo("bg_whiteboard", "Whiteboard", AssetCategory.BACKGROUND, R.drawable.bg_whiteboard),
        AssetInfo("bg_chalkboard", "Chalkboard", AssetCategory.BACKGROUND, R.drawable.bg_chalkboard),
        AssetInfo("bg_paper", "Notepad Paper", AssetCategory.BACKGROUND, R.drawable.bg_paper)
    )
    
    /**
     * All available doodle/icon assets.
     */
    val availableDoodles: List<AssetInfo> = listOf(
        AssetInfo("doodle_arrow", "Arrow", AssetCategory.DOODLE, R.drawable.doodle_arrow),
        AssetInfo("doodle_checkmark", "Checkmark", AssetCategory.DOODLE, R.drawable.doodle_checkmark),
        AssetInfo("doodle_star", "Star", AssetCategory.DOODLE, R.drawable.doodle_star),
        AssetInfo("doodle_lightbulb", "Lightbulb", AssetCategory.DOODLE, R.drawable.doodle_lightbulb),
        AssetInfo("doodle_circle", "Circle", AssetCategory.DOODLE, R.drawable.doodle_circle),
        AssetInfo("doodle_question", "Question Mark", AssetCategory.DOODLE, R.drawable.doodle_question)
    )
    
    /**
     * Get bitmap for a built-in asset by name.
     */
    suspend fun getBuiltInBitmap(
        assetName: String,
        targetWidth: Int = 0,
        targetHeight: Int = 0
    ): Bitmap? = withContext(Dispatchers.IO) {
        val assetInfo = (availableHands + availableBackgrounds + availableDoodles)
            .find { it.name == assetName }
            ?: return@withContext null
        
        val drawable = ContextCompat.getDrawable(context, assetInfo.drawableRes)
            ?: return@withContext null
        
        val width = if (targetWidth > 0) targetWidth else drawable.intrinsicWidth
        val height = if (targetHeight > 0) targetHeight else drawable.intrinsicHeight
        
        drawable.toBitmap(width, height)
    }
    
    /**
     * Get bitmap for hand style.
     */
    suspend fun getHandBitmap(handStyle: HandStyle, size: Int = 200): Bitmap? {
        val assetName = when (handStyle) {
            HandStyle.POINTING -> "hand_pointing"
            HandStyle.WRITING -> "hand_writing"
            HandStyle.CARTOON -> "hand_cartoon"
            HandStyle.NONE -> return null
        }
        return getBuiltInBitmap(assetName, size, size)
    }
    
    /**
     * Get bitmap for background type.
     */
    suspend fun getBackgroundBitmap(
        backgroundType: BackgroundType,
        customPath: String? = null,
        width: Int,
        height: Int
    ): Bitmap? = withContext(Dispatchers.IO) {
        when (backgroundType) {
            BackgroundType.WHITEBOARD -> getBuiltInBitmap("bg_whiteboard", width, height)
            BackgroundType.CHALKBOARD -> getBuiltInBitmap("bg_chalkboard", width, height)
            BackgroundType.PAPER -> getBuiltInBitmap("bg_paper", width, height)
            BackgroundType.CUSTOM -> {
                customPath?.let { loadUserImage(it, width, height) }
            }
        }
    }
    
    /**
     * Load a user image from file path.
     */
    suspend fun loadUserImage(
        path: String,
        targetWidth: Int = 0,
        targetHeight: Int = 0
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            if (!file.exists()) return@withContext null
            
            // Decode with sampling for memory efficiency
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(path, options)
            
            // Calculate sample size
            if (targetWidth > 0 && targetHeight > 0) {
                options.inSampleSize = calculateSampleSize(
                    options.outWidth, options.outHeight,
                    targetWidth, targetHeight
                )
            }
            
            options.inJustDecodeBounds = false
            BitmapFactory.decodeFile(path, options)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Load image from content URI (for gallery picks).
     */
    suspend fun loadFromUri(
        uri: Uri,
        targetWidth: Int = 0,
        targetHeight: Int = 0
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext null
            
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()
            
            if (targetWidth > 0 && targetHeight > 0) {
                options.inSampleSize = calculateSampleSize(
                    options.outWidth, options.outHeight,
                    targetWidth, targetHeight
                )
            }
            
            options.inJustDecodeBounds = false
            val newStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(newStream, null, options)
            newStream?.close()
            
            bitmap
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Copy image from URI to app's internal storage.
     * Returns the new file path.
     */
    suspend fun copyImageToInternal(uri: Uri, projectId: Long): String? = withContext(Dispatchers.IO) {
        try {
            val imagesDir = File(context.filesDir, "project_images/$projectId").apply { mkdirs() }
            val fileName = "img_${System.currentTimeMillis()}.png"
            val outputFile = File(imagesDir, fileName)
            
            context.contentResolver.openInputStream(uri)?.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            outputFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Calculate sample size for efficient bitmap loading.
     */
    private fun calculateSampleSize(
        srcWidth: Int,
        srcHeight: Int,
        targetWidth: Int,
        targetHeight: Int
    ): Int {
        var sampleSize = 1
        if (srcHeight > targetHeight || srcWidth > targetWidth) {
            val halfHeight = srcHeight / 2
            val halfWidth = srcWidth / 2
            
            while ((halfHeight / sampleSize) >= targetHeight &&
                   (halfWidth / sampleSize) >= targetWidth) {
                sampleSize *= 2
            }
        }
        return sampleSize
    }
    
    /**
     * Get resource ID from drawable name.
     */
    fun getDrawableResId(drawableName: String): Int {
        return (availableHands + availableBackgrounds + availableDoodles)
            .find { it.name == drawableName }
            ?.drawableRes
            ?: 0
    }
}

/**
 * Asset information for UI display.
 */
data class AssetInfo(
    val name: String,
    val displayName: String,
    val category: AssetCategory,
    val drawableRes: Int
)

enum class AssetCategory {
    HAND,
    BACKGROUND,
    DOODLE
}

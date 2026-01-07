package com.freewhiteboard.animator.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a user's animation project.
 * Contains metadata and references to scenes.
 */
@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val name: String = "Untitled Project",
    val description: String = "",
    
    // Background settings
    val backgroundType: BackgroundType = BackgroundType.WHITEBOARD,
    val customBackgroundPath: String? = null,
    
    // Hand style
    val handStyle: HandStyle = HandStyle.POINTING,
    
    // Export settings
    val aspectRatio: AspectRatio = AspectRatio.HORIZONTAL_16_9,
    val resolution: Resolution = Resolution.HD_720P,
    
    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    
    // Template info
    val isTemplate: Boolean = false,
    val templateCategory: String? = null
)

/**
 * Represents a single scene in the animation.
 * Each scene has text, optional images, and timing info.
 */
@Entity(
    tableName = "scenes",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class Scene(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val projectId: Long,
    val orderIndex: Int = 0,
    
    // Text content
    val text: String = "",
    val subtitleText: String = "",
    
    // Timing
    val durationMs: Long = 5000, // Default 5 seconds
    val ttsSpeed: Float = 1.0f,
    
    // Image placement
    val imagePlacement: ImagePlacement = ImagePlacement.CENTER,
    
    // Animation settings
    val animationStyle: AnimationStyle = AnimationStyle.HAND_DRAW,
    val transitionStyle: TransitionStyle = TransitionStyle.FADE,
    
    // TTS audio cache path (generated offline)
    val audioPath: String? = null,
    
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Represents an image/asset used in a scene.
 */
@Entity(
    tableName = "scene_assets",
    foreignKeys = [
        ForeignKey(
            entity = Scene::class,
            parentColumns = ["id"],
            childColumns = ["sceneId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sceneId")]
)
data class SceneAsset(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val sceneId: Long,
    
    // Asset source - either built-in drawable or user image path
    val assetType: AssetType = AssetType.BUILT_IN,
    val drawableRes: String? = null, // Resource name for built-in
    val imagePath: String? = null,   // File path for user images
    
    // Positioning
    val placement: ImagePlacement = ImagePlacement.CENTER,
    val scale: Float = 1.0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    
    // Animation
    val animationStyle: AnimationStyle = AnimationStyle.FADE_IN,
    val animationDelayMs: Long = 0,
    val animationDurationMs: Long = 1000
)

// ================== Enums ==================

enum class BackgroundType {
    WHITEBOARD,
    CHALKBOARD,
    PAPER,
    CUSTOM
}

enum class HandStyle {
    POINTING,
    WRITING,
    CARTOON,
    NONE
}

enum class AspectRatio(val width: Int, val height: Int) {
    HORIZONTAL_16_9(16, 9),
    VERTICAL_9_16(9, 16),
    SQUARE_1_1(1, 1)
}

enum class Resolution(val width: Int, val height: Int) {
    SD_480P(854, 480),
    HD_720P(1280, 720),
    FHD_1080P(1920, 1080)
}

enum class ImagePlacement {
    CENTER,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    TOP_CENTER,
    BOTTOM_CENTER,
    LEFT_CENTER,
    RIGHT_CENTER
}

enum class AnimationStyle {
    NONE,
    FADE_IN,
    FADE_OUT,
    ZOOM_IN,
    ZOOM_OUT,
    SLIDE_LEFT,
    SLIDE_RIGHT,
    SLIDE_UP,
    SLIDE_DOWN,
    HAND_DRAW
}

enum class TransitionStyle {
    NONE,
    FADE,
    SLIDE,
    ZOOM
}

enum class AssetType {
    BUILT_IN,
    USER_IMAGE
}

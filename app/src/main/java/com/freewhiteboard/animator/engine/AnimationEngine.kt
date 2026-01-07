package com.freewhiteboard.animator.engine

import android.graphics.*
import android.graphics.drawable.VectorDrawable
import androidx.compose.ui.geometry.Offset
import androidx.core.graphics.drawable.toBitmap
import com.freewhiteboard.animator.data.model.*
import kotlin.math.min

/**
 * Animation engine for creating whiteboard-style animations.
 * Handles hand-drawing effects, image animations, and text rendering.
 */
class AnimationEngine {
    
    /**
     * Render a complete scene frame at the given progress (0.0 to 1.0).
     */
    fun renderFrame(
        canvas: Canvas,
        scene: Scene,
        assets: List<SceneAsset>,
        handBitmap: Bitmap?,
        backgroundBitmap: Bitmap?,
        assetBitmaps: Map<Long, Bitmap>,
        progress: Float, // 0.0 to 1.0
        width: Int,
        height: Int
    ) {
        // 1. Draw background
        drawBackground(canvas, backgroundBitmap, width, height)
        
        // 2. Draw assets with their animations
        assets.forEachIndexed { index, asset ->
            val assetProgress = calculateAssetProgress(progress, index, assets.size)
            assetBitmaps[asset.id]?.let { bitmap ->
                drawAnimatedAsset(canvas, bitmap, asset, assetProgress, width, height)
            }
        }
        
        // 3. Draw text with hand-drawing effect
        if (scene.text.isNotEmpty()) {
            drawTextWithHandEffect(
                canvas = canvas,
                text = scene.text,
                handBitmap = handBitmap,
                progress = progress,
                width = width,
                height = height,
                showHand = scene.animationStyle == AnimationStyle.HAND_DRAW
            )
        }
        
        // 4. Draw subtitle
        if (scene.subtitleText.isNotEmpty()) {
            drawSubtitle(canvas, scene.subtitleText, progress, width, height)
        }
    }
    
    /**
     * Draw background (scaled to fit canvas).
     */
    private fun drawBackground(canvas: Canvas, bitmap: Bitmap?, width: Int, height: Int) {
        if (bitmap != null) {
            val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
            val dstRect = Rect(0, 0, width, height)
            canvas.drawBitmap(bitmap, srcRect, dstRect, null)
        } else {
            // Default white background
            canvas.drawColor(Color.WHITE)
        }
    }
    
    /**
     * Calculate animation progress for each asset (staggered appearance).
     */
    private fun calculateAssetProgress(
        totalProgress: Float,
        assetIndex: Int,
        totalAssets: Int
    ): Float {
        if (totalAssets == 0) return 0f
        
        // Stagger assets so they appear one after another
        val assetDuration = 0.8f / totalAssets
        val assetStart = 0.1f + (assetIndex * assetDuration * 0.7f)
        val assetEnd = assetStart + assetDuration
        
        return ((totalProgress - assetStart) / (assetEnd - assetStart)).coerceIn(0f, 1f)
    }
    
    /**
     * Draw an asset with animation effect.
     */
    private fun drawAnimatedAsset(
        canvas: Canvas,
        bitmap: Bitmap,
        asset: SceneAsset,
        progress: Float,
        canvasWidth: Int,
        canvasHeight: Int
    ) {
        if (progress <= 0f) return
        
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        // Calculate position based on placement
        val position = calculatePlacement(
            asset.placement,
            bitmap.width.toFloat() * asset.scale,
            bitmap.height.toFloat() * asset.scale,
            canvasWidth.toFloat(),
            canvasHeight.toFloat()
        )
        
        canvas.save()
        
        // Apply animation based on style
        when (asset.animationStyle) {
            AnimationStyle.FADE_IN -> {
                paint.alpha = (255 * progress).toInt()
            }
            AnimationStyle.FADE_OUT -> {
                paint.alpha = (255 * (1f - progress)).toInt()
            }
            AnimationStyle.ZOOM_IN -> {
                val scale = progress * asset.scale
                canvas.translate(position.x + bitmap.width * asset.scale / 2, 
                               position.y + bitmap.height * asset.scale / 2)
                canvas.scale(scale / asset.scale, scale / asset.scale)
                canvas.translate(-bitmap.width / 2f, -bitmap.height / 2f)
            }
            AnimationStyle.ZOOM_OUT -> {
                val scale = (1f - progress * 0.5f) * asset.scale
                canvas.translate(position.x + bitmap.width * asset.scale / 2,
                               position.y + bitmap.height * asset.scale / 2)
                canvas.scale(scale / asset.scale, scale / asset.scale)
                canvas.translate(-bitmap.width / 2f, -bitmap.height / 2f)
            }
            AnimationStyle.SLIDE_LEFT -> {
                val offsetX = (1f - progress) * canvasWidth
                canvas.translate(position.x - offsetX, position.y)
                canvas.scale(asset.scale, asset.scale)
            }
            AnimationStyle.SLIDE_RIGHT -> {
                val offsetX = (1f - progress) * -canvasWidth
                canvas.translate(position.x - offsetX, position.y)
                canvas.scale(asset.scale, asset.scale)
            }
            AnimationStyle.SLIDE_UP -> {
                val offsetY = (1f - progress) * canvasHeight
                canvas.translate(position.x, position.y + offsetY)
                canvas.scale(asset.scale, asset.scale)
            }
            AnimationStyle.SLIDE_DOWN -> {
                val offsetY = (1f - progress) * -canvasHeight
                canvas.translate(position.x, position.y + offsetY)
                canvas.scale(asset.scale, asset.scale)
            }
            else -> {
                // No animation or hand draw - just draw at position
                canvas.translate(position.x, position.y)
                canvas.scale(asset.scale, asset.scale)
            }
        }
        
        // Draw the bitmap
        if (asset.animationStyle !in listOf(
            AnimationStyle.ZOOM_IN, AnimationStyle.ZOOM_OUT,
            AnimationStyle.SLIDE_LEFT, AnimationStyle.SLIDE_RIGHT,
            AnimationStyle.SLIDE_UP, AnimationStyle.SLIDE_DOWN
        )) {
            canvas.translate(position.x, position.y)
            canvas.scale(asset.scale, asset.scale)
        }
        
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        canvas.restore()
    }
    
    /**
     * Calculate position based on placement enum.
     */
    private fun calculatePlacement(
        placement: ImagePlacement,
        assetWidth: Float,
        assetHeight: Float,
        canvasWidth: Float,
        canvasHeight: Float
    ): Offset {
        val padding = min(canvasWidth, canvasHeight) * 0.05f
        
        return when (placement) {
            ImagePlacement.CENTER -> Offset(
                (canvasWidth - assetWidth) / 2,
                (canvasHeight - assetHeight) / 2
            )
            ImagePlacement.TOP_LEFT -> Offset(padding, padding)
            ImagePlacement.TOP_RIGHT -> Offset(canvasWidth - assetWidth - padding, padding)
            ImagePlacement.BOTTOM_LEFT -> Offset(padding, canvasHeight - assetHeight - padding)
            ImagePlacement.BOTTOM_RIGHT -> Offset(
                canvasWidth - assetWidth - padding,
                canvasHeight - assetHeight - padding
            )
            ImagePlacement.TOP_CENTER -> Offset((canvasWidth - assetWidth) / 2, padding)
            ImagePlacement.BOTTOM_CENTER -> Offset(
                (canvasWidth - assetWidth) / 2,
                canvasHeight - assetHeight - padding
            )
            ImagePlacement.LEFT_CENTER -> Offset(padding, (canvasHeight - assetHeight) / 2)
            ImagePlacement.RIGHT_CENTER -> Offset(
                canvasWidth - assetWidth - padding,
                (canvasHeight - assetHeight) / 2
            )
        }
    }
    
    /**
     * Draw text with hand-drawing reveal effect.
     */
    private fun drawTextWithHandEffect(
        canvas: Canvas,
        text: String,
        handBitmap: Bitmap?,
        progress: Float,
        width: Int,
        height: Int,
        showHand: Boolean
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = height * 0.06f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        
        // Calculate visible portion of text
        val visibleLength = (text.length * progress).toInt()
        val visibleText = text.take(visibleLength)
        
        // Center the text
        val x = width / 2f
        val y = height / 2f
        
        // Draw revealed text
        canvas.drawText(visibleText, x, y, paint)
        
        // Draw hand at the end of text if animating
        if (showHand && handBitmap != null && progress < 1f && visibleLength > 0) {
            val textWidth = paint.measureText(visibleText)
            val handX = x + textWidth / 2f
            val handY = y - paint.textSize / 2
            val handScale = (height * 0.15f) / handBitmap.height
            
            canvas.save()
            canvas.translate(handX, handY)
            canvas.scale(handScale, handScale)
            canvas.drawBitmap(handBitmap, 0f, 0f, null)
            canvas.restore()
        }
    }
    
    /**
     * Draw subtitle at the bottom of the frame.
     */
    private fun drawSubtitle(
        canvas: Canvas,
        text: String,
        progress: Float,
        width: Int,
        height: Int
    ) {
        // Fade in subtitle
        val alpha = (255 * progress.coerceIn(0f, 1f)).toInt()
        
        // Background bar
        val bgPaint = Paint().apply {
            color = Color.argb(alpha * 180 / 255, 0, 0, 0)
        }
        val bgHeight = height * 0.12f
        canvas.drawRect(0f, height - bgHeight, width.toFloat(), height.toFloat(), bgPaint)
        
        // Subtitle text
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(alpha, 255, 255, 255)
            textSize = height * 0.04f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        
        val x = width / 2f
        val y = height - bgHeight / 2f + textPaint.textSize / 3
        
        canvas.drawText(text, x, y, textPaint)
    }
    
    /**
     * Create transition frame between two scenes.
     */
    fun renderTransition(
        canvas: Canvas,
        transition: TransitionStyle,
        progress: Float, // 0.0 = all scene1, 1.0 = all scene2
        width: Int,
        height: Int,
        scene1Bitmap: Bitmap?,
        scene2Bitmap: Bitmap?
    ) {
        when (transition) {
            TransitionStyle.NONE -> {
                // Instant switch at 0.5
                val bitmap = if (progress < 0.5f) scene1Bitmap else scene2Bitmap
                bitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
            }
            TransitionStyle.FADE -> {
                // Crossfade
                scene1Bitmap?.let {
                    val paint = Paint().apply { alpha = ((1f - progress) * 255).toInt() }
                    canvas.drawBitmap(it, 0f, 0f, paint)
                }
                scene2Bitmap?.let {
                    val paint = Paint().apply { alpha = (progress * 255).toInt() }
                    canvas.drawBitmap(it, 0f, 0f, paint)
                }
            }
            TransitionStyle.SLIDE -> {
                // Slide left
                scene1Bitmap?.let {
                    val offsetX = -width * progress
                    canvas.drawBitmap(it, offsetX, 0f, null)
                }
                scene2Bitmap?.let {
                    val offsetX = width * (1f - progress)
                    canvas.drawBitmap(it, offsetX, 0f, null)
                }
            }
            TransitionStyle.ZOOM -> {
                // Zoom out / in
                scene1Bitmap?.let {
                    canvas.save()
                    val scale = 1f - progress * 0.3f
                    canvas.translate(width / 2f, height / 2f)
                    canvas.scale(scale, scale)
                    canvas.translate(-width / 2f, -height / 2f)
                    val paint = Paint().apply { alpha = ((1f - progress) * 255).toInt() }
                    canvas.drawBitmap(it, 0f, 0f, paint)
                    canvas.restore()
                }
                scene2Bitmap?.let {
                    canvas.save()
                    val scale = 0.7f + progress * 0.3f
                    canvas.translate(width / 2f, height / 2f)
                    canvas.scale(scale, scale)
                    canvas.translate(-width / 2f, -height / 2f)
                    val paint = Paint().apply { alpha = (progress * 255).toInt() }
                    canvas.drawBitmap(it, 0f, 0f, paint)
                    canvas.restore()
                }
            }
        }
    }
}

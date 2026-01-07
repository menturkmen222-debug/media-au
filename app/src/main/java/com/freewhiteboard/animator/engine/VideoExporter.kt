package com.freewhiteboard.animator.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.*
import android.os.Build
import androidx.annotation.RequiresApi
import com.freewhiteboard.animator.data.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.nio.ByteBuffer

/**
 * Video exporter using Android MediaMuxer and MediaCodec.
 * Creates MP4 files from rendered frames (fully offline, no external APIs).
 */
class VideoExporter(private val context: Context) {
    
    private val animationEngine = AnimationEngine()
    
    // Progress state
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()
    
    private val _status = MutableStateFlow(ExportStatus.IDLE)
    val status: StateFlow<ExportStatus> = _status.asStateFlow()
    
    private var exportJob: Job? = null
    
    /**
     * Export project to MP4 video.
     */
    suspend fun exportVideo(
        scenes: List<Scene>,
        assets: Map<Long, List<SceneAsset>>,
        bitmaps: SceneBitmaps,
        outputPath: String,
        resolution: Resolution,
        aspectRatio: AspectRatio,
        fps: Int = 30,
        onProgress: (Float) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.Default) {
        try {
            _status.value = ExportStatus.ENCODING
            _progress.value = 0f
            
            // Calculate dimensions based on aspect ratio and resolution
            val (width, height) = calculateDimensions(resolution, aspectRatio)
            
            // Ensure dimensions are even (required by MediaCodec)
            val videoWidth = (width / 2) * 2
            val videoHeight = (height / 2) * 2
            
            val outputFile = File(outputPath)
            outputFile.parentFile?.mkdirs()
            
            // Calculate total frames
            val totalDurationMs = scenes.sumOf { it.durationMs }
            val totalFrames = ((totalDurationMs / 1000f) * fps).toInt()
            
            // Initialize encoder
            val encoder = createEncoder(videoWidth, videoHeight, fps)
            val muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            
            var videoTrackIndex = -1
            var muxerStarted = false
            val bufferInfo = MediaCodec.BufferInfo()
            
            // Create frame bitmap for rendering
            val frameBitmap = Bitmap.createBitmap(videoWidth, videoHeight, Bitmap.Config.ARGB_8888)
            val frameCanvas = Canvas(frameBitmap)
            
            encoder.start()
            
            var frameIndex = 0
            var currentSceneIndex = 0
            var sceneStartFrame = 0
            
            while (frameIndex < totalFrames) {
                // Check if we need to move to next scene
                val scene = scenes.getOrNull(currentSceneIndex) ?: break
                val sceneFrameCount = ((scene.durationMs / 1000f) * fps).toInt()
                val sceneEndFrame = sceneStartFrame + sceneFrameCount
                
                if (frameIndex >= sceneEndFrame) {
                    currentSceneIndex++
                    sceneStartFrame = sceneEndFrame
                    continue
                }
                
                // Calculate progress within current scene
                val sceneProgress = (frameIndex - sceneStartFrame).toFloat() / sceneFrameCount.coerceAtLeast(1)
                
                // Render frame
                val sceneAssets = assets[scene.id] ?: emptyList()
                animationEngine.renderFrame(
                    canvas = frameCanvas,
                    scene = scene,
                    assets = sceneAssets,
                    handBitmap = bitmaps.handBitmaps[scene.projectId],
                    backgroundBitmap = bitmaps.backgroundBitmaps[scene.projectId],
                    assetBitmaps = bitmaps.assetBitmaps,
                    progress = sceneProgress,
                    width = videoWidth,
                    height = videoHeight
                )
                
                // Encode frame
                val inputBufferIndex = encoder.dequeueInputBuffer(10000)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = encoder.getInputBuffer(inputBufferIndex)
                    inputBuffer?.clear()
                    
                    // Convert bitmap to NV21/YUV format for encoder
                    val pixelBuffer = ByteBuffer.allocate(videoWidth * videoHeight * 4)
                    frameBitmap.copyPixelsToBuffer(pixelBuffer)
                    pixelBuffer.rewind()
                    
                    // For simplicity, encode as raw ARGB (some devices support this)
                    inputBuffer?.put(pixelBuffer)
                    
                    val presentationTimeUs = (frameIndex * 1_000_000L) / fps
                    encoder.queueInputBuffer(inputBufferIndex, 0, pixelBuffer.capacity(), presentationTimeUs, 0)
                }
                
                // Get encoded data
                var outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000)
                while (outputBufferIndex >= 0) {
                    val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)
                    
                    if (!muxerStarted) {
                        val format = encoder.outputFormat
                        videoTrackIndex = muxer.addTrack(format)
                        muxer.start()
                        muxerStarted = true
                    }
                    
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                        outputBuffer?.let {
                            it.position(bufferInfo.offset)
                            it.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(videoTrackIndex, it, bufferInfo)
                        }
                    }
                    
                    encoder.releaseOutputBuffer(outputBufferIndex, false)
                    outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 0)
                }
                
                // Update progress
                frameIndex++
                val currentProgress = frameIndex.toFloat() / totalFrames
                _progress.value = currentProgress
                onProgress(currentProgress)
                
                // Yield to allow cancellation
                yield()
            }
            
            // Signal end of stream
            val inputBufferIndex = encoder.dequeueInputBuffer(10000)
            if (inputBufferIndex >= 0) {
                encoder.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            }
            
            // Drain remaining output
            var outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000)
            while (outputBufferIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (outputBufferIndex >= 0) {
                    val outputBuffer = encoder.getOutputBuffer(outputBufferIndex)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 && muxerStarted) {
                        outputBuffer?.let {
                            muxer.writeSampleData(videoTrackIndex, it, bufferInfo)
                        }
                    }
                    encoder.releaseOutputBuffer(outputBufferIndex, false)
                    
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        break
                    }
                }
                outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000)
            }
            
            // Cleanup
            encoder.stop()
            encoder.release()
            if (muxerStarted) {
                muxer.stop()
                muxer.release()
            }
            frameBitmap.recycle()
            
            _status.value = ExportStatus.COMPLETE
            _progress.value = 1f
            
            Result.success(outputPath)
        } catch (e: Exception) {
            _status.value = ExportStatus.ERROR
            Result.failure(e)
        }
    }
    
    /**
     * Create video encoder with H.264.
     */
    private fun createEncoder(width: Int, height: Int, fps: Int): MediaCodec {
        val mimeType = MediaFormat.MIMETYPE_VIDEO_AVC
        val format = MediaFormat.createVideoFormat(mimeType, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, 
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, width * height * 4) // ~4 bits per pixel
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        
        val encoder = MediaCodec.createEncoderByType(mimeType)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        return encoder
    }
    
    /**
     * Calculate output dimensions based on resolution and aspect ratio.
     */
    private fun calculateDimensions(resolution: Resolution, aspectRatio: AspectRatio): Pair<Int, Int> {
        return when (aspectRatio) {
            AspectRatio.HORIZONTAL_16_9 -> Pair(resolution.width, resolution.height)
            AspectRatio.VERTICAL_9_16 -> Pair(resolution.height, resolution.width)
            AspectRatio.SQUARE_1_1 -> {
                val size = minOf(resolution.width, resolution.height)
                Pair(size, size)
            }
        }
    }
    
    /**
     * Cancel ongoing export.
     */
    fun cancel() {
        exportJob?.cancel()
        _status.value = ExportStatus.CANCELLED
    }
    
    enum class ExportStatus {
        IDLE,
        ENCODING,
        MUXING_AUDIO,
        COMPLETE,
        ERROR,
        CANCELLED
    }
}

/**
 * Container for all bitmaps needed for rendering.
 */
data class SceneBitmaps(
    val handBitmaps: Map<Long, Bitmap> = emptyMap(),
    val backgroundBitmaps: Map<Long, Bitmap> = emptyMap(),
    val assetBitmaps: Map<Long, Bitmap> = emptyMap()
)

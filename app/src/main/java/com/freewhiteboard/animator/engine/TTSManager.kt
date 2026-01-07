package com.freewhiteboard.animator.engine

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Manager for Android Text-to-Speech functionality.
 * Generates audio files for scenes using system TTS (fully offline).
 */
class TTSManager(private val context: Context) {
    
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var currentLocale: Locale = Locale.getDefault()
    
    /**
     * Initialize TTS engine. Call before using other methods.
     */
    suspend fun initialize(): Boolean = suspendCancellableCoroutine { cont ->
        tts = TextToSpeech(context) { status ->
            isInitialized = status == TextToSpeech.SUCCESS
            if (isInitialized) {
                tts?.language = currentLocale
            }
            if (cont.isActive) {
                cont.resume(isInitialized)
            }
        }
        
        cont.invokeOnCancellation {
            tts?.shutdown()
            tts = null
        }
    }
    
    /**
     * Get list of available voices/languages.
     */
    fun getAvailableLocales(): List<Locale> {
        return tts?.availableLanguages?.toList() ?: listOf(Locale.getDefault())
    }
    
    /**
     * Set the TTS language.
     */
    fun setLanguage(locale: Locale): Boolean {
        val result = tts?.setLanguage(locale)
        val success = result != TextToSpeech.LANG_MISSING_DATA && 
                      result != TextToSpeech.LANG_NOT_SUPPORTED
        if (success) {
            currentLocale = locale
        }
        return success
    }
    
    /**
     * Set speech rate (0.5 = half speed, 1.0 = normal, 2.0 = double speed).
     */
    fun setSpeechRate(rate: Float) {
        tts?.setSpeechRate(rate.coerceIn(0.5f, 2.0f))
    }
    
    /**
     * Set pitch (0.5 = low, 1.0 = normal, 2.0 = high).
     */
    fun setPitch(pitch: Float) {
        tts?.setPitch(pitch.coerceIn(0.5f, 2.0f))
    }
    
    /**
     * Generate audio file from text (offline).
     * Returns the path to the generated WAV file.
     */
    suspend fun generateAudio(
        text: String,
        outputFileName: String,
        speechRate: Float = 1.0f
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!isInitialized || tts == null) {
            return@withContext Result.failure(Exception("TTS not initialized"))
        }
        
        val audioDir = File(context.filesDir, "audio").apply { mkdirs() }
        val outputFile = File(audioDir, "$outputFileName.wav")
        
        tts?.setSpeechRate(speechRate)
        
        suspendCancellableCoroutine { cont ->
            val utteranceId = "tts_${System.currentTimeMillis()}"
            
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(uttId: String?) {}
                
                override fun onDone(uttId: String?) {
                    if (uttId == utteranceId && cont.isActive) {
                        cont.resume(Result.success(outputFile.absolutePath))
                    }
                }
                
                @Deprecated("Deprecated in API")
                override fun onError(uttId: String?) {
                    if (uttId == utteranceId && cont.isActive) {
                        cont.resume(Result.failure(Exception("TTS synthesis failed")))
                    }
                }
                
                override fun onError(uttId: String?, errorCode: Int) {
                    if (uttId == utteranceId && cont.isActive) {
                        cont.resume(Result.failure(Exception("TTS error code: $errorCode")))
                    }
                }
            })
            
            val result = tts?.synthesizeToFile(
                text,
                null,
                outputFile,
                utteranceId
            )
            
            if (result != TextToSpeech.SUCCESS) {
                if (cont.isActive) {
                    cont.resume(Result.failure(Exception("Failed to start TTS synthesis")))
                }
            }
        }
    }
    
    /**
     * Estimate duration of speech in milliseconds.
     */
    fun estimateDuration(text: String, speechRate: Float = 1.0f): Long {
        // Average speaking rate is ~150 words per minute
        // Adjust based on speech rate setting
        val wordCount = text.split(Regex("\\s+")).size
        val baseSeconds = wordCount / 2.5f // ~150 wpm
        val adjustedSeconds = baseSeconds / speechRate
        return (adjustedSeconds * 1000).toLong().coerceAtLeast(1000)
    }
    
    /**
     * Speak text directly (for preview).
     */
    fun speak(text: String, speechRate: Float = 1.0f) {
        tts?.setSpeechRate(speechRate)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "preview_${System.currentTimeMillis()}")
    }
    
    /**
     * Stop any ongoing speech.
     */
    fun stop() {
        tts?.stop()
    }
    
    /**
     * Clean up resources.
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
    
    /**
     * Delete cached audio files.
     */
    fun clearCache() {
        val audioDir = File(context.filesDir, "audio")
        audioDir.listFiles()?.forEach { it.delete() }
    }
}

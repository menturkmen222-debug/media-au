package com.freewhiteboard.animator.ui.screens

import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.freewhiteboard.animator.R
import com.freewhiteboard.animator.WhiteboardAnimatorApp
import com.freewhiteboard.animator.data.model.*
import com.freewhiteboard.animator.engine.VideoExporter
import kotlinx.coroutines.launch
import java.io.File

/**
 * Export screen for exporting project to MP4 video.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    projectId: Long,
    onExportComplete: () -> Unit,
    onBack: () -> Unit
) {
    val app = WhiteboardAnimatorApp.getInstance()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val project by app.repository.getProjectFlow(projectId).collectAsStateWithLifecycle(initialValue = null)
    val scenes by app.repository.getScenesFlow(projectId).collectAsStateWithLifecycle(initialValue = emptyList())
    
    var selectedResolution by remember { mutableStateOf(Resolution.HD_720P) }
    var selectedAspectRatio by remember { mutableStateOf(AspectRatio.HORIZONTAL_16_9) }
    
    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableStateOf(0f) }
    var exportError by remember { mutableStateOf<String?>(null) }
    var exportSuccess by remember { mutableStateOf(false) }
    var exportedFilePath by remember { mutableStateOf<String?>(null) }
    
    val videoExporter = remember { VideoExporter(context) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.export_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isExporting) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Project info card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = project?.name ?: "Project",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${scenes.size} scenes • ${calculateTotalDuration(scenes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Resolution selection
            Text(
                text = stringResource(R.string.resolution),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Resolution.entries.forEach { resolution ->
                    FilterChip(
                        selected = selectedResolution == resolution,
                        onClick = { selectedResolution = resolution },
                        label = { 
                            Text(
                                when (resolution) {
                                    Resolution.SD_480P -> "480p"
                                    Resolution.HD_720P -> "720p HD"
                                    Resolution.FHD_1080P -> "1080p Full HD"
                                }
                            ) 
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Aspect ratio selection
            Text(
                text = stringResource(R.string.aspect_ratio),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AspectRatioChip(
                    label = stringResource(R.string.horizontal_16_9),
                    icon = Icons.Outlined.Crop169,
                    selected = selectedAspectRatio == AspectRatio.HORIZONTAL_16_9,
                    onClick = { selectedAspectRatio = AspectRatio.HORIZONTAL_16_9 },
                    modifier = Modifier.weight(1f)
                )
                AspectRatioChip(
                    label = stringResource(R.string.vertical_9_16),
                    icon = Icons.Outlined.CropPortrait,
                    selected = selectedAspectRatio == AspectRatio.VERTICAL_9_16,
                    onClick = { selectedAspectRatio = AspectRatio.VERTICAL_9_16 },
                    modifier = Modifier.weight(1f)
                )
                AspectRatioChip(
                    label = stringResource(R.string.square_1_1),
                    icon = Icons.Outlined.CropSquare,
                    selected = selectedAspectRatio == AspectRatio.SQUARE_1_1,
                    onClick = { selectedAspectRatio = AspectRatio.SQUARE_1_1 },
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Social media presets
            Text(
                text = "Social Media Presets",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SuggestionChip(
                    onClick = { 
                        selectedAspectRatio = AspectRatio.HORIZONTAL_16_9
                        selectedResolution = Resolution.FHD_1080P
                    },
                    label = { Text("YouTube") },
                    icon = { Icon(Icons.Outlined.YouTube, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                SuggestionChip(
                    onClick = { 
                        selectedAspectRatio = AspectRatio.VERTICAL_9_16
                        selectedResolution = Resolution.FHD_1080P
                    },
                    label = { Text("TikTok/Reels") },
                    icon = { Icon(Icons.Outlined.Smartphone, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                SuggestionChip(
                    onClick = { 
                        selectedAspectRatio = AspectRatio.SQUARE_1_1
                        selectedResolution = Resolution.HD_720P
                    },
                    label = { Text("Instagram") },
                    icon = { Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Export progress or button
            if (isExporting) {
                ExportProgressCard(progress = exportProgress)
            } else if (exportSuccess) {
                ExportSuccessCard(
                    filePath = exportedFilePath ?: "",
                    onDone = onExportComplete
                )
            } else {
                if (exportError != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = exportError ?: stringResource(R.string.error_export),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                
                Button(
                    onClick = {
                        scope.launch {
                            isExporting = true
                            exportError = null
                            
                            try {
                                // Prepare output path
                                val fileName = "${project?.name?.replace(" ", "_") ?: "video"}_${System.currentTimeMillis()}.mp4"
                                val outputDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "exports")
                                outputDir.mkdirs()
                                val outputPath = File(outputDir, fileName).absolutePath
                                
                                // Get assets for all scenes
                                val assets = scenes.associate { scene ->
                                    scene.id to app.repository.getAssets(scene.id)
                                }
                                
                                // Export video
                                val result = videoExporter.exportVideo(
                                    scenes = scenes,
                                    assets = assets,
                                    bitmaps = com.freewhiteboard.animator.engine.SceneBitmaps(),
                                    outputPath = outputPath,
                                    resolution = selectedResolution,
                                    aspectRatio = selectedAspectRatio,
                                    onProgress = { progress ->
                                        exportProgress = progress
                                    }
                                )
                                
                                result.fold(
                                    onSuccess = { path ->
                                        exportedFilePath = path
                                        exportSuccess = true
                                    },
                                    onFailure = { error ->
                                        exportError = error.message ?: "Export failed"
                                    }
                                )
                            } catch (e: Exception) {
                                exportError = e.message ?: "Export failed"
                            }
                            
                            isExporting = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = scenes.isNotEmpty()
                ) {
                    Icon(Icons.Default.Movie, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.export), style = MaterialTheme.typography.titleMedium)
                }
                
                if (scenes.isEmpty()) {
                    Text(
                        text = "Add scenes to export a video",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

@Composable
private fun AspectRatioChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { 
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.height(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        },
        modifier = modifier
    )
}

@Composable
private fun ExportProgressCard(progress: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "progress")
    val animatedProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress_animation"
    )
    
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(80.dp),
                strokeWidth = 8.dp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = stringResource(R.string.export_progress),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
private fun ExportSuccessCard(
    filePath: String,
    onDone: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.export_complete),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Video saved to Movies/exports",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Done, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Done")
            }
        }
    }
}

private fun calculateTotalDuration(scenes: List<Scene>): String {
    val totalMs = scenes.sumOf { it.durationMs }
    val seconds = (totalMs / 1000) % 60
    val minutes = (totalMs / 1000) / 60
    return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
}

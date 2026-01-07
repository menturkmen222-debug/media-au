package com.freewhiteboard.animator.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.freewhiteboard.animator.R
import com.freewhiteboard.animator.WhiteboardAnimatorApp
import com.freewhiteboard.animator.data.model.*
import com.freewhiteboard.animator.engine.AnimationEngine
import kotlinx.coroutines.launch

/**
 * Main editor screen with timeline, scene editing, and preview.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    projectId: Long,
    onAddAssets: (Long) -> Unit,
    onExport: () -> Unit,
    onBack: () -> Unit
) {
    val app = WhiteboardAnimatorApp.getInstance()
    val scope = rememberCoroutineScope()
    
    val project by app.repository.getProjectFlow(projectId).collectAsStateWithLifecycle(initialValue = null)
    val scenes by app.repository.getScenesFlow(projectId).collectAsStateWithLifecycle(initialValue = emptyList())
    
    var selectedSceneId by remember { mutableStateOf<Long?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var previewProgress by remember { mutableStateOf(0f) }
    
    // Preview bitmap
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    val selectedScene = scenes.find { it.id == selectedSceneId }
    
    // Select first scene by default
    LaunchedEffect(scenes) {
        if (selectedSceneId == null && scenes.isNotEmpty()) {
            selectedSceneId = scenes.first().id
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = project?.name ?: "Editor",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            val id = app.repository.addScene(projectId)
                            selectedSceneId = id
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_scene))
                    }
                    FilledTonalButton(
                        onClick = onExport,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.export))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Preview area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                // Preview canvas
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap!!.asImageBitmap(),
                        contentDescription = "Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    // Placeholder
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayCircle,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (scenes.isEmpty()) "Add scenes to preview" 
                                   else "Select a scene",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Play controls overlay
                if (scenes.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledIconButton(
                            onClick = { isPlaying = !isPlaying }
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) stringResource(R.string.pause) else stringResource(R.string.play)
                            )
                        }
                    }
                }
            }
            
            // Progress slider
            if (scenes.isNotEmpty()) {
                Slider(
                    value = previewProgress,
                    onValueChange = { previewProgress = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
            
            HorizontalDivider()
            
            // Timeline section title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Timeline",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${scenes.size} scenes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Scene timeline (horizontal scroll)
            if (scenes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.AddCircle,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No scenes yet",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(130.dp)
                ) {
                    itemsIndexed(scenes, key = { _, scene -> scene.id }) { index, scene ->
                        SceneTimelineCard(
                            scene = scene,
                            index = index,
                            isSelected = scene.id == selectedSceneId,
                            onClick = { selectedSceneId = scene.id },
                            onEdit = {
                                selectedSceneId = scene.id
                                showEditDialog = true
                            }
                        )
                    }
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            
            // Selected scene details
            selectedScene?.let { scene ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Scene ${scenes.indexOf(scene) + 1}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Scene text
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Text",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = scene.text.ifEmpty { "(No text)" },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Quick settings row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedCard(
                            onClick = { showEditDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Outlined.Timer, contentDescription = null)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${scene.durationMs / 1000}s",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                        
                        OutlinedCard(
                            onClick = { onAddAssets(scene.id) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Outlined.Image, contentDescription = null)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.add_image),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                        
                        OutlinedCard(
                            onClick = { /* TTS preview */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Outlined.VolumeUp, contentDescription = null)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Voice",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showEditDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.edit_text))
                        }
                        
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    app.repository.deleteScene(scene.id)
                                    selectedSceneId = scenes.firstOrNull { it.id != scene.id }?.id
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.delete_scene))
                        }
                    }
                }
            }
        }
    }
    
    // Edit scene dialog
    if (showEditDialog && selectedScene != null) {
        EditSceneDialog(
            scene = selectedScene!!,
            onDismiss = { showEditDialog = false },
            onSave = { updatedScene ->
                scope.launch {
                    app.repository.updateScene(updatedScene)
                    showEditDialog = false
                }
            }
        )
    }
}

/**
 * Timeline card for a single scene.
 */
@Composable
private fun SceneTimelineCard(
    scene: Scene,
    index: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(140.dp)
            .fillMaxHeight(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = if (isSelected) 
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
        else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "${index + 1}",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(
                    text = "${scene.durationMs / 1000}s",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = scene.text.ifEmpty { "Empty scene" },
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            
            IconButton(
                onClick = onEdit,
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.End)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Edit",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Dialog for editing scene properties.
 */
@Composable
private fun EditSceneDialog(
    scene: Scene,
    onDismiss: () -> Unit,
    onSave: (Scene) -> Unit
) {
    var text by remember { mutableStateOf(scene.text) }
    var durationSeconds by remember { mutableStateOf((scene.durationMs / 1000).toInt()) }
    var ttsSpeed by remember { mutableStateOf(scene.ttsSpeed) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Scene") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Scene Text") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                
                Text("Duration: ${durationSeconds}s", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = durationSeconds.toFloat(),
                    onValueChange = { durationSeconds = it.toInt() },
                    valueRange = 1f..30f,
                    steps = 28
                )
                
                Text("TTS Speed: ${String.format("%.1f", ttsSpeed)}x", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = ttsSpeed,
                    onValueChange = { ttsSpeed = it },
                    valueRange = 0.5f..2f
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(scene.copy(
                    text = text,
                    subtitleText = text,
                    durationMs = durationSeconds * 1000L,
                    ttsSpeed = ttsSpeed
                ))
            }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

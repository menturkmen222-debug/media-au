package com.freewhiteboard.animator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.freewhiteboard.animator.R
import com.freewhiteboard.animator.WhiteboardAnimatorApp
import kotlinx.coroutines.launch

/**
 * Screen for entering script text that gets auto-split into scenes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptInputScreen(
    projectId: Long,
    onScenesCreated: () -> Unit,
    onBack: () -> Unit
) {
    val app = WhiteboardAnimatorApp.getInstance()
    val scope = rememberCoroutineScope()
    
    var scriptText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var previewScenes by remember { mutableStateOf<List<String>>(emptyList()) }
    
    // Update preview when script changes
    LaunchedEffect(scriptText) {
        previewScenes = splitScriptPreview(scriptText)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Write Your Script") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${previewScenes.size} scenes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Button(
                        onClick = {
                            scope.launch {
                                isProcessing = true
                                app.repository.createScenesFromScript(projectId, scriptText)
                                isProcessing = false
                                onScenesCreated()
                            }
                        },
                        enabled = scriptText.isNotBlank() && !isProcessing
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.generate_scenes))
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Instructions card
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tips",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Separate scenes with blank lines (paragraphs)\n" +
                               "• Or use sentences - each will become a scene\n" +
                               "• Keep scenes short for better readability",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Script input field
            OutlinedTextField(
                value = scriptText,
                onValueChange = { scriptText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 250.dp)
                    .padding(horizontal = 16.dp),
                placeholder = { Text(stringResource(R.string.script_hint)) },
                minLines = 10
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Scene preview
            if (previewScenes.isNotEmpty()) {
                Text(
                    text = "Preview Scenes",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                previewScenes.forEachIndexed { index, scene ->
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = scene.take(100) + if (scene.length > 100) "..." else "",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Preview script splitting logic.
 */
private fun splitScriptPreview(script: String): List<String> {
    if (script.isBlank()) return emptyList()
    
    // Try paragraph splitting first
    val paragraphs = script.split(Regex("\\n\\s*\\n"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    
    if (paragraphs.size > 1) {
        return paragraphs
    }
    
    // Fall back to sentence splitting
    return script.split(Regex("(?<=[.!?])\\s+"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}

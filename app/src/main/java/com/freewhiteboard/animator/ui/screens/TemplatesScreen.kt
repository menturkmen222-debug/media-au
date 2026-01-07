package com.freewhiteboard.animator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.freewhiteboard.animator.R
import com.freewhiteboard.animator.WhiteboardAnimatorApp
import com.freewhiteboard.animator.data.model.Project
import kotlinx.coroutines.launch

/**
 * Templates screen for browsing and using pre-made project templates.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    onTemplateSelected: (Long) -> Unit,
    onBack: () -> Unit
) {
    val app = WhiteboardAnimatorApp.getInstance()
    val scope = rememberCoroutineScope()
    
    val templates by app.repository.allTemplates.collectAsStateWithLifecycle(initialValue = emptyList())
    
    var showNameDialog by remember { mutableStateOf(false) }
    var selectedTemplate by remember { mutableStateOf<Project?>(null) }
    
    // Create sample templates if none exist
    LaunchedEffect(Unit) {
        if (templates.isEmpty()) {
            createSampleTemplates(app)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.templates)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (templates.isEmpty()) {
            // Loading or empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading templates...")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(templates, key = { it.id }) { template ->
                    TemplateCard(
                        template = template,
                        onClick = {
                            selectedTemplate = template
                            showNameDialog = true
                        }
                    )
                }
            }
        }
    }
    
    // New project from template dialog
    if (showNameDialog && selectedTemplate != null) {
        var projectName by remember { mutableStateOf(selectedTemplate!!.name) }
        
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Create from Template") },
            text = {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text(stringResource(R.string.project_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val newProjectId = app.repository.createProjectFromTemplate(
                                templateId = selectedTemplate!!.id,
                                name = projectName.ifBlank { selectedTemplate!!.name }
                            )
                            showNameDialog = false
                            if (newProjectId > 0) {
                                onTemplateSelected(newProjectId)
                            }
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun TemplateCard(
    template: Project,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = MaterialTheme.shapes.medium,
                color = getCategoryColor(template.templateCategory)
            ) {
                Icon(
                    imageVector = getCategoryIcon(template.templateCategory),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxSize(),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = template.description.ifEmpty { 
                        template.templateCategory ?: "Template" 
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun getCategoryColor(category: String?): androidx.compose.ui.graphics.Color {
    return when (category) {
        "Education" -> MaterialTheme.colorScheme.primaryContainer
        "Business" -> MaterialTheme.colorScheme.secondaryContainer
        "Marketing" -> MaterialTheme.colorScheme.tertiaryContainer
        "Tutorial" -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
}

private fun getCategoryIcon(category: String?): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category) {
        "Education" -> Icons.Outlined.School
        "Business" -> Icons.Outlined.Business
        "Marketing" -> Icons.Outlined.Campaign
        "Tutorial" -> Icons.Outlined.MenuBook
        else -> Icons.Outlined.AutoAwesome
    }
}

/**
 * Create sample templates for new users.
 */
private suspend fun createSampleTemplates(app: WhiteboardAnimatorApp) {
    // Education template
    val eduProject = app.repository.saveProject(
        com.freewhiteboard.animator.data.model.Project(
            name = "Explainer Video",
            description = "Perfect for educational content",
            isTemplate = true,
            templateCategory = "Education"
        )
    )
    app.repository.createScenesFromScript(
        eduProject,
        "Welcome to this explainer video.\n\n" +
        "Today we'll learn about an important topic.\n\n" +
        "Here's the first key point to understand.\n\n" +
        "And here's the second key point.\n\n" +
        "Let's summarize what we've learned."
    )
    
    // Business template
    val bizProject = app.repository.saveProject(
        com.freewhiteboard.animator.data.model.Project(
            name = "Product Demo",
            description = "Showcase your product features",
            isTemplate = true,
            templateCategory = "Business"
        )
    )
    app.repository.createScenesFromScript(
        bizProject,
        "Introducing our amazing product.\n\n" +
        "Feature one: Fast and efficient.\n\n" +
        "Feature two: Easy to use.\n\n" +
        "Feature three: Affordable pricing.\n\n" +
        "Get started today!"
    )
    
    // Marketing template
    val mktProject = app.repository.saveProject(
        com.freewhiteboard.animator.data.model.Project(
            name = "Social Media Ad",
            description = "Short and engaging content",
            isTemplate = true,
            templateCategory = "Marketing"
        )
    )
    app.repository.createScenesFromScript(
        mktProject,
        "Are you struggling with this problem?\n\n" +
        "We have the solution!\n\n" +
        "Here's how it works.\n\n" +
        "Try it free today!"
    )
    
    // Tutorial template
    val tutProject = app.repository.saveProject(
        com.freewhiteboard.animator.data.model.Project(
            name = "How-To Tutorial",
            description = "Step-by-step instructions",
            isTemplate = true,
            templateCategory = "Tutorial"
        )
    )
    app.repository.createScenesFromScript(
        tutProject,
        "In this tutorial, you'll learn how to do something amazing.\n\n" +
        "Step 1: Prepare everything you need.\n\n" +
        "Step 2: Follow these instructions.\n\n" +
        "Step 3: Complete the final steps.\n\n" +
        "Congratulations! You did it!"
    )
}

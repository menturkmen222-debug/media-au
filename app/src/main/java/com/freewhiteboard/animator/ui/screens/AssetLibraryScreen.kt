package com.freewhiteboard.animator.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.freewhiteboard.animator.R
import com.freewhiteboard.animator.WhiteboardAnimatorApp
import com.freewhiteboard.animator.data.model.*
import com.freewhiteboard.animator.engine.AssetCategory
import com.freewhiteboard.animator.engine.AssetInfo
import kotlinx.coroutines.launch

/**
 * Asset library screen for browsing and selecting images.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetLibraryScreen(
    projectId: Long,
    sceneId: Long,
    onAssetSelected: () -> Unit,
    onBack: () -> Unit
) {
    val app = WhiteboardAnimatorApp.getInstance()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Doodles", "Hands", "Backgrounds", "My Images")
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val path = app.assetManager.copyImageToInternal(it, projectId)
                if (path != null) {
                    app.repository.addUserImage(
                        sceneId = sceneId,
                        imagePath = path,
                        placement = ImagePlacement.CENTER,
                        animationStyle = AnimationStyle.FADE_IN
                    )
                    onAssetSelected()
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.assets_library)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
        ) {
            // Tab row
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            when (selectedTab) {
                0 -> AssetGrid(
                    assets = app.assetManager.availableDoodles,
                    onAssetClick = { asset ->
                        scope.launch {
                            app.repository.addBuiltInAsset(
                                sceneId = sceneId,
                                drawableName = asset.name,
                                placement = getSmartPlacement(sceneId),
                                animationStyle = AnimationStyle.FADE_IN
                            )
                            onAssetSelected()
                        }
                    }
                )
                1 -> AssetGrid(
                    assets = app.assetManager.availableHands,
                    onAssetClick = { asset ->
                        // Hands are used for the project, not individual scenes
                        scope.launch {
                            val project = app.repository.getProject(projectId)
                            project?.let {
                                val handStyle = when (asset.name) {
                                    "hand_pointing" -> HandStyle.POINTING
                                    "hand_writing" -> HandStyle.WRITING
                                    "hand_cartoon" -> HandStyle.CARTOON
                                    else -> HandStyle.POINTING
                                }
                                app.repository.updateProject(it.copy(handStyle = handStyle))
                            }
                            onAssetSelected()
                        }
                    }
                )
                2 -> AssetGrid(
                    assets = app.assetManager.availableBackgrounds,
                    onAssetClick = { asset ->
                        scope.launch {
                            val project = app.repository.getProject(projectId)
                            project?.let {
                                val bgType = when (asset.name) {
                                    "bg_whiteboard" -> BackgroundType.WHITEBOARD
                                    "bg_chalkboard" -> BackgroundType.CHALKBOARD
                                    "bg_paper" -> BackgroundType.PAPER
                                    else -> BackgroundType.WHITEBOARD
                                }
                                app.repository.updateProject(it.copy(backgroundType = bgType))
                            }
                            onAssetSelected()
                        }
                    }
                )
                3 -> MyImagesTab(
                    onPickImage = { imagePickerLauncher.launch("image/*") }
                )
            }
        }
    }
}

/**
 * Grid of built-in assets.
 */
@Composable
private fun AssetGrid(
    assets: List<AssetInfo>,
    onAssetClick: (AssetInfo) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(assets) { asset ->
            AssetCard(
                asset = asset,
                onClick = { onAssetClick(asset) }
            )
        }
    }
}

/**
 * Card for a single asset.
 */
@Composable
private fun AssetCard(
    asset: AssetInfo,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = asset.drawableRes),
                contentDescription = asset.displayName,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentScale = ContentScale.Fit
            )
        }
        
        // Label at bottom
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f)
        ) {
            Text(
                text = asset.displayName,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * Tab for user's own images.
 */
@Composable
private fun MyImagesTab(
    onPickImage: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.AddPhotoAlternate,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Add images from your gallery",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onPickImage) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Choose Image")
            }
        }
    }
}

/**
 * Smart placement algorithm - alternates corners for visual variety.
 */
private suspend fun getSmartPlacement(sceneId: Long): ImagePlacement {
    val app = WhiteboardAnimatorApp.getInstance()
    val existingAssets = app.repository.getAssets(sceneId)
    
    val placements = listOf(
        ImagePlacement.TOP_LEFT,
        ImagePlacement.TOP_RIGHT,
        ImagePlacement.BOTTOM_LEFT,
        ImagePlacement.BOTTOM_RIGHT,
        ImagePlacement.CENTER
    )
    
    // Find first unused placement
    val usedPlacements = existingAssets.map { it.placement }.toSet()
    return placements.firstOrNull { it !in usedPlacements } ?: ImagePlacement.CENTER
}

package com.freewhiteboard.animator.data.repository

import com.freewhiteboard.animator.data.database.ProjectDao
import com.freewhiteboard.animator.data.database.SceneDao
import com.freewhiteboard.animator.data.database.SceneAssetDao
import com.freewhiteboard.animator.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Repository for managing whiteboard animation projects.
 * Provides a clean API for data operations with auto-save support.
 */
class ProjectRepository(
    private val projectDao: ProjectDao,
    private val sceneDao: SceneDao,
    private val sceneAssetDao: SceneAssetDao
) {
    
    // ================== Projects ==================
    
    val allProjects: Flow<List<Project>> = projectDao.getAllProjects()
    val allTemplates: Flow<List<Project>> = projectDao.getAllTemplates()
    
    fun getProjectFlow(projectId: Long): Flow<Project?> = 
        projectDao.getProjectByIdFlow(projectId)
    
    suspend fun getProject(projectId: Long): Project? = 
        projectDao.getProjectById(projectId)
    
    suspend fun createProject(name: String = "Untitled Project"): Long {
        val project = Project(name = name)
        return projectDao.insertProject(project)
    }
    
    suspend fun saveProject(project: Project): Long {
        val updated = project.copy(updatedAt = System.currentTimeMillis())
        return projectDao.insertProject(updated)
    }
    
    suspend fun updateProject(project: Project) {
        val updated = project.copy(updatedAt = System.currentTimeMillis())
        projectDao.updateProject(updated)
    }
    
    suspend fun deleteProject(projectId: Long) {
        projectDao.deleteProjectById(projectId)
    }
    
    // ================== Scenes ==================
    
    fun getScenesFlow(projectId: Long): Flow<List<Scene>> = 
        sceneDao.getScenesForProject(projectId)
    
    suspend fun getScenes(projectId: Long): List<Scene> = 
        sceneDao.getScenesForProjectSync(projectId)
    
    suspend fun getScene(sceneId: Long): Scene? = 
        sceneDao.getSceneById(sceneId)
    
    /**
     * Create scenes from script text.
     * Splits by sentences/paragraphs automatically.
     */
    suspend fun createScenesFromScript(projectId: Long, script: String): List<Long> {
        // Split script into scenes (by paragraph or double newline, fallback to sentences)
        val segments = splitScriptToScenes(script)
        
        val maxIndex = sceneDao.getMaxOrderIndex(projectId) ?: -1
        
        val scenes = segments.mapIndexed { index, text ->
            Scene(
                projectId = projectId,
                orderIndex = maxIndex + index + 1,
                text = text.trim(),
                subtitleText = text.trim(),
                durationMs = calculateDuration(text)
            )
        }
        
        sceneDao.insertScenes(scenes)
        
        // Update project timestamp
        projectDao.updateTimestamp(projectId)
        
        // Return IDs (need to fetch them since bulk insert doesn't return IDs)
        return sceneDao.getScenesForProjectSync(projectId)
            .filter { it.orderIndex > maxIndex }
            .map { it.id }
    }
    
    suspend fun addScene(projectId: Long, text: String = ""): Long {
        val maxIndex = sceneDao.getMaxOrderIndex(projectId) ?: -1
        val scene = Scene(
            projectId = projectId,
            orderIndex = maxIndex + 1,
            text = text,
            subtitleText = text
        )
        val id = sceneDao.insertScene(scene)
        projectDao.updateTimestamp(projectId)
        return id
    }
    
    suspend fun updateScene(scene: Scene) {
        sceneDao.updateScene(scene)
        projectDao.updateTimestamp(scene.projectId)
    }
    
    suspend fun deleteScene(sceneId: Long) {
        val scene = sceneDao.getSceneById(sceneId)
        if (scene != null) {
            sceneDao.deleteScene(scene)
            projectDao.updateTimestamp(scene.projectId)
        }
    }
    
    suspend fun reorderScenes(projectId: Long, sceneIds: List<Long>) {
        sceneIds.forEachIndexed { index, sceneId ->
            sceneDao.updateSceneOrder(sceneId, index)
        }
        projectDao.updateTimestamp(projectId)
    }
    
    // ================== Assets ==================
    
    fun getAssetsFlow(sceneId: Long): Flow<List<SceneAsset>> = 
        sceneAssetDao.getAssetsForScene(sceneId)
    
    suspend fun getAssets(sceneId: Long): List<SceneAsset> = 
        sceneAssetDao.getAssetsForSceneSync(sceneId)
    
    suspend fun addBuiltInAsset(
        sceneId: Long, 
        drawableName: String,
        placement: ImagePlacement = ImagePlacement.CENTER,
        animationStyle: AnimationStyle = AnimationStyle.FADE_IN
    ): Long {
        val asset = SceneAsset(
            sceneId = sceneId,
            assetType = AssetType.BUILT_IN,
            drawableRes = drawableName,
            placement = placement,
            animationStyle = animationStyle
        )
        return sceneAssetDao.insertAsset(asset)
    }
    
    suspend fun addUserImage(
        sceneId: Long,
        imagePath: String,
        placement: ImagePlacement = ImagePlacement.CENTER,
        animationStyle: AnimationStyle = AnimationStyle.FADE_IN
    ): Long {
        val asset = SceneAsset(
            sceneId = sceneId,
            assetType = AssetType.USER_IMAGE,
            imagePath = imagePath,
            placement = placement,
            animationStyle = animationStyle
        )
        return sceneAssetDao.insertAsset(asset)
    }
    
    suspend fun updateAsset(asset: SceneAsset) {
        sceneAssetDao.updateAsset(asset)
    }
    
    suspend fun deleteAsset(assetId: Long) {
        sceneAssetDao.deleteAssetById(assetId)
    }
    
    // ================== Templates ==================
    
    suspend fun createTemplate(project: Project, category: String): Long {
        val template = project.copy(
            id = 0, // New ID
            isTemplate = true,
            templateCategory = category,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return projectDao.insertProject(template)
    }
    
    suspend fun createProjectFromTemplate(templateId: Long, name: String): Long {
        val template = projectDao.getProjectById(templateId) ?: return -1
        
        // Create new project from template
        val project = template.copy(
            id = 0,
            name = name,
            isTemplate = false,
            templateCategory = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val projectId = projectDao.insertProject(project)
        
        // Copy scenes
        val templateScenes = sceneDao.getScenesForProjectSync(templateId)
        templateScenes.forEach { scene ->
            val newScene = scene.copy(id = 0, projectId = projectId)
            val newSceneId = sceneDao.insertScene(newScene)
            
            // Copy assets
            val assets = sceneAssetDao.getAssetsForSceneSync(scene.id)
            val newAssets = assets.map { it.copy(id = 0, sceneId = newSceneId) }
            sceneAssetDao.insertAssets(newAssets)
        }
        
        return projectId
    }
    
    // ================== Helpers ==================
    
    /**
     * Split script text into scene segments.
     * Prioritizes paragraph breaks, falls back to sentence splitting.
     */
    private fun splitScriptToScenes(script: String): List<String> {
        // First try splitting by double newlines (paragraphs)
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
    
    /**
     * Calculate estimated duration for a text segment based on word count.
     * Assumes ~150 words per minute speaking rate.
     */
    private fun calculateDuration(text: String): Long {
        val wordCount = text.split(Regex("\\s+")).size
        val seconds = (wordCount / 2.5f).coerceIn(3f, 30f)
        return (seconds * 1000).toLong()
    }
}

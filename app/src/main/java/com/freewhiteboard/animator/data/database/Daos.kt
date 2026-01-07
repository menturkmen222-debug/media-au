package com.freewhiteboard.animator.data.database

import androidx.room.*
import com.freewhiteboard.animator.data.model.Project
import com.freewhiteboard.animator.data.model.Scene
import com.freewhiteboard.animator.data.model.SceneAsset
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Project operations.
 */
@Dao
interface ProjectDao {
    
    @Query("SELECT * FROM projects WHERE isTemplate = 0 ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<Project>>
    
    @Query("SELECT * FROM projects WHERE isTemplate = 1 ORDER BY name ASC")
    fun getAllTemplates(): Flow<List<Project>>
    
    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: Long): Project?
    
    @Query("SELECT * FROM projects WHERE id = :id")
    fun getProjectByIdFlow(id: Long): Flow<Project?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project): Long
    
    @Update
    suspend fun updateProject(project: Project)
    
    @Delete
    suspend fun deleteProject(project: Project)
    
    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)
    
    @Query("UPDATE projects SET updatedAt = :timestamp WHERE id = :id")
    suspend fun updateTimestamp(id: Long, timestamp: Long = System.currentTimeMillis())
}

/**
 * Data Access Object for Scene operations.
 */
@Dao
interface SceneDao {
    
    @Query("SELECT * FROM scenes WHERE projectId = :projectId ORDER BY orderIndex ASC")
    fun getScenesForProject(projectId: Long): Flow<List<Scene>>
    
    @Query("SELECT * FROM scenes WHERE projectId = :projectId ORDER BY orderIndex ASC")
    suspend fun getScenesForProjectSync(projectId: Long): List<Scene>
    
    @Query("SELECT * FROM scenes WHERE id = :id")
    suspend fun getSceneById(id: Long): Scene?
    
    @Query("SELECT MAX(orderIndex) FROM scenes WHERE projectId = :projectId")
    suspend fun getMaxOrderIndex(projectId: Long): Int?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScene(scene: Scene): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScenes(scenes: List<Scene>)
    
    @Update
    suspend fun updateScene(scene: Scene)
    
    @Delete
    suspend fun deleteScene(scene: Scene)
    
    @Query("DELETE FROM scenes WHERE id = :id")
    suspend fun deleteSceneById(id: Long)
    
    @Query("DELETE FROM scenes WHERE projectId = :projectId")
    suspend fun deleteAllScenesForProject(projectId: Long)
    
    @Query("UPDATE scenes SET orderIndex = :newIndex WHERE id = :sceneId")
    suspend fun updateSceneOrder(sceneId: Long, newIndex: Int)
}

/**
 * Data Access Object for SceneAsset operations.
 */
@Dao
interface SceneAssetDao {
    
    @Query("SELECT * FROM scene_assets WHERE sceneId = :sceneId ORDER BY id ASC")
    fun getAssetsForScene(sceneId: Long): Flow<List<SceneAsset>>
    
    @Query("SELECT * FROM scene_assets WHERE sceneId = :sceneId ORDER BY id ASC")
    suspend fun getAssetsForSceneSync(sceneId: Long): List<SceneAsset>
    
    @Query("SELECT * FROM scene_assets WHERE id = :id")
    suspend fun getAssetById(id: Long): SceneAsset?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: SceneAsset): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssets(assets: List<SceneAsset>)
    
    @Update
    suspend fun updateAsset(asset: SceneAsset)
    
    @Delete
    suspend fun deleteAsset(asset: SceneAsset)
    
    @Query("DELETE FROM scene_assets WHERE id = :id")
    suspend fun deleteAssetById(id: Long)
    
    @Query("DELETE FROM scene_assets WHERE sceneId = :sceneId")
    suspend fun deleteAllAssetsForScene(sceneId: Long)
}

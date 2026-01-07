package com.freewhiteboard.animator.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.freewhiteboard.animator.data.model.Project
import com.freewhiteboard.animator.data.model.Scene
import com.freewhiteboard.animator.data.model.SceneAsset

/**
 * Main Room database for the Whiteboard Animator app.
 * Stores projects, scenes, and scene assets.
 */
@Database(
    entities = [Project::class, Scene::class, SceneAsset::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun projectDao(): ProjectDao
    abstract fun sceneDao(): SceneDao
    abstract fun sceneAssetDao(): SceneAssetDao
    
    companion object {
        private const val DATABASE_NAME = "whiteboard_animator.db"
        
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        /**
         * Get or create the database instance (singleton pattern).
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }
        
        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}

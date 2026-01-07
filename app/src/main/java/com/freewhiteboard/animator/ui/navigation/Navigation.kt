package com.freewhiteboard.animator.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.freewhiteboard.animator.ui.screens.*

/**
 * App navigation routes.
 */
object Routes {
    const val HOME = "home"
    const val SCRIPT_INPUT = "script_input/{projectId}"
    const val EDITOR = "editor/{projectId}"
    const val ASSET_LIBRARY = "asset_library/{projectId}/{sceneId}"
    const val EXPORT = "export/{projectId}"
    const val TEMPLATES = "templates"
    
    fun scriptInput(projectId: Long) = "script_input/$projectId"
    fun editor(projectId: Long) = "editor/$projectId"
    fun assetLibrary(projectId: Long, sceneId: Long) = "asset_library/$projectId/$sceneId"
    fun export(projectId: Long) = "export/$projectId"
}

/**
 * Main navigation composable.
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        // Home screen - project list
        composable(Routes.HOME) {
            HomeScreen(
                onCreateProject = { projectId ->
                    navController.navigate(Routes.scriptInput(projectId))
                },
                onOpenProject = { projectId ->
                    navController.navigate(Routes.editor(projectId))
                },
                onOpenTemplates = {
                    navController.navigate(Routes.TEMPLATES)
                }
            )
        }
        
        // Script input screen
        composable(
            route = Routes.SCRIPT_INPUT,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: return@composable
            ScriptInputScreen(
                projectId = projectId,
                onScenesCreated = {
                    navController.navigate(Routes.editor(projectId)) {
                        popUpTo(Routes.HOME)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        // Editor screen - timeline and scene editing
        composable(
            route = Routes.EDITOR,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: return@composable
            EditorScreen(
                projectId = projectId,
                onAddAssets = { sceneId ->
                    navController.navigate(Routes.assetLibrary(projectId, sceneId))
                },
                onExport = {
                    navController.navigate(Routes.export(projectId))
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        // Asset library screen
        composable(
            route = Routes.ASSET_LIBRARY,
            arguments = listOf(
                navArgument("projectId") { type = NavType.LongType },
                navArgument("sceneId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: return@composable
            val sceneId = backStackEntry.arguments?.getLong("sceneId") ?: return@composable
            AssetLibraryScreen(
                projectId = projectId,
                sceneId = sceneId,
                onAssetSelected = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        
        // Export screen
        composable(
            route = Routes.EXPORT,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: return@composable
            ExportScreen(
                projectId = projectId,
                onExportComplete = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        
        // Templates screen
        composable(Routes.TEMPLATES) {
            TemplatesScreen(
                onTemplateSelected = { projectId ->
                    navController.navigate(Routes.editor(projectId)) {
                        popUpTo(Routes.HOME)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}

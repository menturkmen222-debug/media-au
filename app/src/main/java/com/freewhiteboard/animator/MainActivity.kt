package com.freewhiteboard.animator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.freewhiteboard.animator.ui.navigation.AppNavigation
import com.freewhiteboard.animator.ui.theme.WhiteboardAnimatorTheme

/**
 * Main activity for the Whiteboard Animator app.
 * Uses edge-to-edge display and Compose navigation.
 */
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        enableEdgeToEdge()
        
        setContent {
            WhiteboardAnimatorTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}

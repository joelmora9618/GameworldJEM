package com.jem.gameworldjem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.jem.gameworldjem.ui.GameWorldScreen
import com.jem.gameworldjem.ui.theme.GameworldJEMTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GameworldJEMTheme { // o MaterialTheme si prefieres
                MaterialTheme {
                    GameWorldScreen(
                        title = "GameWorld Demo",
                        onBack = null,                 // o { finish() }
                        basePath = "tiled",            // carpeta dentro de assets/
                        mapFile = "demo_map.tmj",      // archivo del mapa
                        heroSheetPath = "tiled/hero_spritesheet.png",
                        desiredTilesTall = 1.6f        // zoom del personaje (opcional)
                    )
                }
            }
        }
    }
}

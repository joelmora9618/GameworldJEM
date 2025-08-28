package com.jem.gameworldjem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.jem.gameworldjem.GameWorldScreen
import com.jem.gameworldjem.ui.theme.GameworldJEMTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                GameWorldScreen(
                    onBack = null, // o { finish() },
                    mapPath = "tiled/demo_map.tmj",
                    heroSheetPath = "tiled/hero_spritesheet.png",
                    title = "GameWorld Demo"
                )
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GameworldJEMTheme {
        Greeting("Android")
    }
}
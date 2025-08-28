package com.jem.gameworldjem.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun MovementPad(
    modifier: Modifier = Modifier,
    onDir: (Float, Float) -> Unit,
    onRelease: () -> Unit
) {
    Column(modifier) {
        Row(Modifier.height(48.dp)) {
            Spacer(Modifier.width(48.dp))
            ControlBtn("↑", { onDir(0f, -1f) }, onRelease)
            Spacer(Modifier.width(48.dp))
        }
        Row {
            ControlBtn("←", { onDir(-1f, 0f) }, onRelease)
            Spacer(Modifier.width(8.dp))
            ControlBtn("↓", { onDir(0f, 1f) }, onRelease)
            Spacer(Modifier.width(8.dp))
            ControlBtn("→", { onDir(1f, 0f) }, onRelease)
        }
    }
}

@Composable
internal fun ControlBtn(label: String, onPress: () -> Unit, onRelease: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    LaunchedEffect(pressed) { if (pressed) onPress() else onRelease() }
    Button(
        onClick = {},
        interactionSource = interaction,
        modifier = Modifier.size(48.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F))
    ) { Text(label, color = Color.White) }
}
